package com.mmos.mmos.src.service;

import com.mmos.mmos.src.domain.dto.plan.PlanIsCompleteRequestDto;
import com.mmos.mmos.src.domain.dto.plan.PlanNameUpdateRequestDto;
import com.mmos.mmos.src.domain.dto.plan.PlanResponseDto;
import com.mmos.mmos.src.domain.dto.plan.PlanSaveRequestDto;
import com.mmos.mmos.src.domain.entity.*;
import com.mmos.mmos.src.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.mmos.mmos.config.HttpResponseStatus.*;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlannerRepository plannerRepository;
    private final UserStudyRepository userStudyRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private final PlannerService plannerService;
    private final CalendarService calendarService;

    public Planner findPlannerByIdx(Long plannerIdx) {
        return plannerRepository.findById(plannerIdx)
                .orElse(null);
    }

    public UserStudy findUserStudyByIdx(Long userStudyIdx) {
        return userStudyRepository.findById(userStudyIdx)
                .orElse(null);
    }

    public Plan findPlanByIdx(Long planIdx) {
        return planRepository.findById(planIdx)
                .orElse(null);
    }

    public List<Plan> findPlansByPlannerAndPlanIsStudy(Planner planner, Boolean isStudy) {
        return planRepository.findPlansByPlannerAndPlanIsStudy(planner, isStudy)
                .orElse(null);
    }

    public Planner findPlannerByCalendarIdxAndDate(Long calendarIdx, LocalDate date) {
        return plannerRepository.findPlannerByCalendar_CalendarIndexAndPlannerDate(calendarIdx, date)
                .orElse(null);
    }

    public Calendar findCalendarByUserIdx(Long userIdx, int year, int month) {
        return calendarRepository.findCalendarByUser_UserIndexAndCalendarYearAndCalendarMonth(userIdx, year, month)
                .orElse(null);
    }

    public List<Plan> findPlansByPlanner(Planner planner) {
        return planRepository.findPlansByPlanner(planner)
                .orElse(null);
    }

    @Transactional
    public PlanResponseDto savePlan(PlanSaveRequestDto requestDto, Long userIdx) {

        UserStudy userStudy = null;
        if (requestDto.getIsStudy()) {
            userStudy = findUserStudyByIdx(requestDto.getUserStudyIdx());
            if(userStudy == null)
                return new PlanResponseDto(USERSTUDY_NOT_EXIST_USERSTUDY);
        }

        // 저장하려는 월의 캘린더가 존재하는지 확인
        // 존재한다면 DB에서 가져오고, 존재하지 않는다면 DB에 새로 저장 후, 저장한 값 가져오기
        Long calendarIdx;
        Calendar calendar = findCalendarByUserIdx(userIdx, requestDto.getDate().getYear(), requestDto.getDate().getMonthValue());
        if (calendar == null) {
            calendarIdx = calendarService.saveCalendar(requestDto.getDate().getYear(), requestDto.getDate().getMonthValue(), userIdx).getIdx();
        } else {
            calendarIdx = calendar.getCalendarIndex();
        }
        // 저장하려는 날의 플래너가 존재하는지 확인
        // 존재한다면 DB에서 가져오고, 존재하지 않는다면 DB에 새로 저장 후, 저장한 값 가져오기
        Long plannerIdx;
        Planner planner = findPlannerByCalendarIdxAndDate(calendarIdx, requestDto.getDate());
        if (planner == null) {
            plannerIdx = plannerService.savePlanner(requestDto.getDate(), calendarIdx).getIdx();
        } else {
            plannerIdx = planner.getPlannerIndex();
        }

        // Plan 객체 생성
        planner = findPlannerByIdx(plannerIdx);
        if(planner == null)
            return new PlanResponseDto(EMPTY_PLANNER);
        Plan plan = new Plan(requestDto, planner, userStudy);

        // 역 FK 매핑
        planner.addPlan(plan);
        if(userStudy != null)
            userStudy.addPlan(plan);

        return new PlanResponseDto(planRepository.save(plan), SUCCESS);

    }

    @Transactional
    public PlanResponseDto getPlan(Long planIdx) {
        Plan plan = findPlanByIdx(planIdx);

        if(plan == null)
            return new PlanResponseDto(EMPTY_PLAN);
        return new PlanResponseDto(plan, SUCCESS);
    }

    // 스터디에 포함된 계획 = true
    // 스터디에 포함되지 않은 계획 = false
    @Transactional
    public List<PlanResponseDto> getPlansByPlanIsStudy(Long plannerIdx, Boolean isStudy) {
        Planner planner = findPlannerByIdx(plannerIdx);  // Planner 객체를 찾는다
        if(planner == null)
            return null;
        List<Plan> planList = findPlansByPlannerAndPlanIsStudy(planner, isStudy);

        List<PlanResponseDto> responseDtoList = new ArrayList<>();
        for (Plan plan : planList) {
            responseDtoList.add(new PlanResponseDto(plan, SUCCESS));
        }

        return responseDtoList;
    }

    @Transactional
    public List<PlanResponseDto> getPlans(Long plannerIdx) {
        Planner planner = findPlannerByIdx(plannerIdx);  // Planner 객체를 찾는다
        if(planner == null)
            return null;
        List<Plan> plans = findPlansByPlanner(planner);  // 해당 Planner와 관련된 Plan 객체들을 찾는다

        List<PlanResponseDto> responseDtoList = new ArrayList<>();
        for (Plan plan : plans) {
            responseDtoList.add(new PlanResponseDto(plan, SUCCESS));
        }

        return responseDtoList;
    }

    @Transactional
    public PlanResponseDto updatePlan(Long planIdx, PlanNameUpdateRequestDto requestDto) {
        Plan plan = findPlanByIdx(planIdx);
        if(plan == null)
            return new PlanResponseDto(EMPTY_PLAN);

        plan.update(requestDto.getPlanName());

        return new PlanResponseDto(plan, SUCCESS);
    }

    @Transactional
    public PlanResponseDto deletePlan(Long planIdx) {
        Plan plan = findPlanByIdx(planIdx);
        if(plan == null)
            return new PlanResponseDto(EMPTY_PLAN);

        plan.getPlanner().minusTime(plan.getPlanStudyTime());
        plan.getPlanner().getCalendar().minusTime(plan.getPlanStudyTime());
        if(plan.getPlanIsStudy())
            plan.getUserStudy().getStudy().minusAverageStudyTime(plan.getPlanStudyTime());

        planRepository.delete(plan);

        return new PlanResponseDto(plan, SUCCESS);
    }

    // plan 완수 여부 기능
    @Transactional
    public PlanResponseDto updatePlanIsComplete(Long planIdx, PlanIsCompleteRequestDto requestDto) {
        Plan plan = findPlanByIdx(planIdx);
        if(plan == null)
            return new PlanResponseDto(EMPTY_PLAN);
        // 같은 경우 제외
        if (plan.getPlanIsComplete() == requestDto.getIsComplete()) {
            return new PlanResponseDto(UPDATE_PLAN_REDUNDANT_REQUEST);
        }

        plan.updateIsComplete(requestDto.getIsComplete());
        Planner planner = plan.getPlanner();
        Calendar calendar = planner.getCalendar();


        // 일별/월별 완료한 플랜 갯수 조정 (0보다 작은데 decrease 하는 case 방지)
        if (planner.getPlannerDailyScheduleNum() >= 0 && calendar.getCalendarMonthlyCompletedPlanNum() >= 0) {
            planner.updateDailyScheduleNum(requestDto.getIsComplete());
            calendar.updateMonthlyPlanNum(requestDto.getIsComplete());
        }

        return new PlanResponseDto(plan, SUCCESS);

    }

    // 플랜이 캘린더에 표시되는 것
    @Transactional
    public PlanResponseDto updatePlanIsVisible(Long planIdx) {
        Plan plan = findPlanByIdx(planIdx);
        if(plan == null)
            return new PlanResponseDto(EMPTY_PLAN);
        Planner planner = plan.getPlanner();
        User user = plan.getPlanner().getCalendar().getUser();

        Long visiblePlanCount = planRepository.countByPlannerAndPlanIsVisibleTrue(planner);
        Long visibleProjectCount = projectRepository.countByUserAndProjectIsVisibleTrue(user);

        if (!plan.getPlanIsVisible()) {
            if (visiblePlanCount + visibleProjectCount >= 5) {
                return new PlanResponseDto(POST_PLAN_ISVISIBLE_FULL);
            }

            plan.updateIsVisible(true);
        } else {
            plan.updateIsVisible(false);
        }

        return new PlanResponseDto(plan, SUCCESS);
    }

}
