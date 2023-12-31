package com.mmos.mmos.src.service;

import com.mmos.mmos.src.domain.dto.project.*;
import com.mmos.mmos.src.domain.entity.*;
import com.mmos.mmos.src.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.mmos.mmos.config.HttpResponseStatus.*;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarService calendarService;
    private final PlanRepository planRepository;
    private final StudyRepository studyRepository;

    public User findUser(Long userIdx) {
        return userRepository.findById(userIdx)
                .orElse(null);
    }

    public Calendar findCalendar(Long userIdx, int year, int month) {
        return calendarRepository.findCalendarByUser_UserIndexAndCalendarYearAndCalendarMonth(userIdx, year, month)
                .orElse(null);
    }

    public Project findProject(Long projectIdx){
        return projectRepository.findById(projectIdx)
                .orElse(null);
    }

    public Study findStudy(Long studyIdx) {
        return studyRepository.findById(studyIdx)
                .orElse(null);
    }


    @Transactional
    public ProjectResponseDto saveProject(Long userIdx, ProjectSaveRequestDto projectSaveRequestDto) {
        // user 찾기
        User user = findUser(userIdx);
        if(user == null)
            return new ProjectResponseDto(EMPTY_USER);


        // 프로젝트 시작시각부터 종료시각까지 필요한 달 체크. 없으면 생성.
        LocalDate startTime = projectSaveRequestDto.getStartTime();
        LocalDate endTime = projectSaveRequestDto.getEndTime();
        for (LocalDate time = startTime; (time.getMonthValue() <= endTime.getMonthValue()) || (time.getYear() < endTime.getYear()); time = time.plusMonths(1)) {
            Calendar calendar = findCalendar(userIdx, time.getYear(), time.getMonthValue());
            System.out.println("calendar = " + calendar);
            if (calendar == null) {
                calendarService.saveCalendar(time.getYear(), time.getMonthValue(), userIdx);
            }
        }

        // 프로젝트 생성
        Project project = new Project(startTime, endTime, projectSaveRequestDto.getName(), user);

        //isStudy 검사
        /*
        Boolean isStudy = projectSaveRequestDto.getIsStudy();
        Long studyIdx = projectSaveRequestDto.getStudyIdx();

        if (projectSaveRequestDto.getIsStudy()) {
            if (projectSaveRequestDto.getStudyIdx() == null) {
                return new ProjectResponseDto(EMPTY_STUDY);
            }
            Study study = findStudy(projectSaveRequestDto.getStudyIdx());
            if (study == null) {
                return new ProjectResponseDto(EMPTY_PROJECT);
            }
            project.setStudy(study);
        } else {
            project.setStudy(null);
        }
        */

        // User에 프로젝트 저장
        user.getUserProjects().add(project);

        return new ProjectResponseDto(projectRepository.save(project), SUCCESS);
    }

    @Transactional
    public ProjectResponseDto updateProjectTime(Long userIdx, Long projectIdx, ProjectTimeUpdateDto projectTimeUpdateDto) {
        // 프로젝트를 소유한 유저인지 확인
        Project project = findProject(projectIdx);
        if(project == null)
            return new ProjectResponseDto(EMPTY_PROJECT);
        User user = findUser(userIdx);
        if(user == null)
            return new ProjectResponseDto(EMPTY_USER);
        if(!user.getUserProjects().contains(project)){
            return new ProjectResponseDto(UPDATE_PROJECT_NOT_OWNER);
        }

        project.updateProjectStartTime(projectTimeUpdateDto.getNewStartTime());
        project.updateProjectEndTime(projectTimeUpdateDto.getNewEndTime());

        return new ProjectResponseDto(project, SUCCESS);
    }
    @Transactional
    public ProjectResponseDto updateProjectName(Long userIdx, Long projectIdx, ProjectNameUpdateDto projectNameUpdateDto) {
        // 프로젝트를 소유한 유저인지 확인
        Project project = findProject(projectIdx);
        if(project == null)
            return new ProjectResponseDto(EMPTY_PROJECT);
        User user = findUser(userIdx);
        if(user == null)
            return new ProjectResponseDto(EMPTY_USER);
        if(!user.getUserProjects().contains(project)){
            return new ProjectResponseDto(UPDATE_PROJECT_NOT_OWNER);
        }

        project.updateProjectName(projectNameUpdateDto.getNewName());

        return new ProjectResponseDto(project, SUCCESS);
    }

    @Transactional
    public ProjectResponseDto updateProjectIsComplete(Long userIdx, Long projectIdx, ProjectStatusUpdateDto projectCompleteUpdateDto) {
        // 프로젝트를 소유한 유저인지 확인
        Project project = findProject(projectIdx);
        if(project == null)
            return new ProjectResponseDto(EMPTY_PROJECT);
        User user = findUser(userIdx);
        if(user == null)
            return new ProjectResponseDto(EMPTY_USER);
        if(!user.getUserProjects().contains(project)){
            return new ProjectResponseDto(UPDATE_PROJECT_NOT_OWNER);
        }
        project.updateProjectIsComplete(projectCompleteUpdateDto.getStatus());

        return new ProjectResponseDto(project, SUCCESS);
    }
    @Transactional
    public ProjectResponseDto updateProjectIsVisible(Long userIdx, Long projectIdx, ProjectStatusUpdateDto projectCompleteUpdateDto) {
        // 프로젝트를 소유한 유저인지 확인
        Project project = findProject(projectIdx);
        if(project == null)
            return new ProjectResponseDto(EMPTY_PROJECT);
        User user = findUser(userIdx);
        if(user == null)
            return new ProjectResponseDto(EMPTY_USER);
        if(!user.getUserProjects().contains(project)){
            return new ProjectResponseDto(UPDATE_PROJECT_NOT_OWNER);
        }
        Calendar calendar = findCalendar(userIdx,project.getProjectStartTime().getYear(),project.getProjectStartTime().getMonthValue());
        List<Planner> plannerList = calendar.getCalendarPlanners();
        // 플래너 isVisble 5개 넘는지 체크
        for (LocalDate date = project.getProjectStartTime(); date.isBefore(project.getProjectEndTime().plusDays(1)); date = date.plusDays(1)){
            // 달 지나면 새로운 캘린더 가져오기
            if(date.getMonthValue() != calendar.getCalendarMonth()) {
                calendar = findCalendar(userIdx, date.getYear(), date.getMonthValue());
                plannerList = calendar.getCalendarPlanners();
            }

            // 프로젝트 일정을 걸친 날들의 is visible == true 플랜 + 프로젝트 개수 세기
            for (Planner planner : plannerList) {
                if(planner.getPlannerDate().equals(date)){
                    int count = 0;
                    for (Plan plannerPlan : planner.getPlannerPlans()) {
                        if(plannerPlan.getPlanIsVisible()){
                            count++;
                        }
                        if(count >= 5)
                            return new ProjectResponseDto(UPDATE_PROJECT_FULL_VISIBLE);
                    }
                    for (Project userProject : user.getUserProjects()) {
                        if((userProject.getProjectStartTime().isBefore(date)||userProject.getProjectStartTime().isEqual(date)) && (userProject.getProjectEndTime().isAfter(date)||userProject.getProjectEndTime().isEqual(date))) {
                            if (userProject.getProjectIsVisible()) {
                                count++;
                            }
                            if(count >= 5)
                                return new ProjectResponseDto(UPDATE_PROJECT_FULL_VISIBLE);
                        }
                    }
                    break;
                }
            }
        }

        project.updateProjectIsVisible(projectCompleteUpdateDto.getStatus());
        return new ProjectResponseDto(project, SUCCESS);
    }
    @Transactional
    public Long deleteProject(Long userIdx, Long projectIdx){
        // 프로젝트를 소유한 유저인지 확인
        Project project = findProject(projectIdx);
        if(project == null)
            return -3L;
        User user = findUser(userIdx);
        if(user == null)
            return -1L;
        if(!user.getUserProjects().contains(project)){
            return -2L;
        }

        projectRepository.delete(project);

        return projectIdx;
    }
}
