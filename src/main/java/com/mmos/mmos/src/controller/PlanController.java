package com.mmos.mmos.src.controller;

import com.mmos.mmos.config.ResponseApiMessage;
import com.mmos.mmos.src.domain.dto.plan.PlanIsCompleteRequestDto;
import com.mmos.mmos.src.domain.dto.plan.PlanNameUpdateRequestDto;
import com.mmos.mmos.src.domain.dto.plan.PlanResponseDto;
import com.mmos.mmos.src.domain.dto.plan.PlanSaveRequestDto;
import com.mmos.mmos.src.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mmos.mmos.config.HttpResponseStatus.*;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController extends BaseController {

    private final PlanService planService;

    /**
     * 계획 추가 버튼 눌렀을 때 실행되는 API (완료)
     *      존재하지 않거나 알맞지 않는 userStudyIdx가 입력된 경우에 버그가 나더라도 잘못된 데이터로 저장되지만
     *      해당 케이스는 스터디 단에서 보내는 것이므로, 이 문제와는 연관이 없음.
     * @param userIdx: 유저 인덱스
     * @param requestDto
     *          - String planName: 계획 이름
     *          - Boolean isStudy: 스터디 계획인지 아닌지
     *          - Long userStudyIdx: 만약 스터디 계획이라면, 유저 스터디 인덱스
     *          - LocalDate date: 계획한 날짜
     */
    // 일반 계획 생성
    @ResponseBody
    @PostMapping("/{userIdx}")
    public ResponseEntity<ResponseApiMessage> savePlan(@PathVariable Long userIdx, @RequestBody PlanSaveRequestDto requestDto) {
        // 계획에 내용이 없을 시 발동되는 조건.
        if(requestDto.getPlanName() == null){
            return sendResponseHttpByJson(POST_PLAN_EMPTY_CONTENTS, "EMPTY PLAN_NAME.", requestDto);
        }
        // 스터디 계획이 아닌데 스터디 인덱스가 있는 경우를 막는 Validation
        if(!requestDto.getIsStudy() && requestDto.getUserStudyIdx() != null || requestDto.getIsStudy() && requestDto.getUserStudyIdx() == null)
            return sendResponseHttpByJson(POST_PLAN_INVALID_REQUEST, "isStudy == false && userStudyIdx != null.", requestDto);

        PlanResponseDto responseDto = planService.savePlan(requestDto, userIdx);

        if(responseDto.getStatus().equals(EMPTY_PLANNER))
            return sendResponseHttpByJson(EMPTY_PLANNER, "존재하지 않는 플래너입니다.", null);
        if(responseDto.getStatus().equals(USERSTUDY_NOT_EXIST_USERSTUDY))
            return sendResponseHttpByJson(USERSTUDY_NOT_EXIST_USERSTUDY, "참여 중인 스터디가 아닙니다.", null);
        return sendResponseHttpByJson(SUCCESS, "Saved Plan.", responseDto);
    }

    /**
     * 내 계획 중 인덱스로 하나만 조회하는 API (완료)
     * @param planIdx: 계획 인덱스
     */
    // 계획 조회 (단일)
    @ResponseBody
    @GetMapping("/{planIdx}")
    public ResponseEntity<ResponseApiMessage> getPlan(@PathVariable Long planIdx) {
        PlanResponseDto responseDto = planService.getPlan(planIdx);

        return sendResponseHttpByJson(SUCCESS, "GET PLAN. PLAN_INDEX = " + planIdx, responseDto);
    }

    /**
     * 내 계획 중 스터디 관련 계획들 조회하는 API (완료)
     * @param : plannerIdx, planIsStudy
     * @return
     */
    // 계획 조회(조건부)
    // 스터디 관련 계획이면 true, 그렇지 않으면 false
    @ResponseBody
    @GetMapping("/{plannerIdx}/{planIsStudy}")
    public ResponseEntity<ResponseApiMessage> getPlansByIsStudy(@PathVariable Long plannerIdx, @PathVariable Boolean planIsStudy){
        List<PlanResponseDto> responseDtoList = planService.getPlansByPlanIsStudy(plannerIdx, planIsStudy);

        if(responseDtoList == null)
            return sendResponseHttpByJson(EMPTY_PLANNER, "존재하지 않는 플래너입니다.", null);
        return sendResponseHttpByJson(SUCCESS, "Load Plans.", responseDtoList);
    }

    /**
     * 내 계획 전체 조회하는 API (완료)
     * @param : plannerIdx
     * @return
     */
    // 계획 조회 (전체)
    @ResponseBody
    @GetMapping("/all/{plannerIdx}")
    public ResponseEntity<ResponseApiMessage> getPlans(@PathVariable Long plannerIdx) {
        List<PlanResponseDto> responseDtoList = planService.getPlans(plannerIdx);

        if(responseDtoList == null)
            return sendResponseHttpByJson(EMPTY_PLANNER, "존재하지 않는 플래너입니다.", null);
        return sendResponseHttpByJson(SUCCESS, "GET PLANS.", responseDtoList);
    }

    /**
     * 내 계획 수정하는 API (완료)
     * @param planIdx: 계획 인덱스
     * @param requestDto
     *           String planName: 계획 이름
     * @return
     */
    // 계획 내용 수정
    @ResponseBody
    @PatchMapping("/nameupdate/{planIdx}")
    public ResponseEntity<ResponseApiMessage> updatePlan(@PathVariable Long planIdx, @RequestBody PlanNameUpdateRequestDto requestDto) {
        PlanResponseDto responseDto = planService.updatePlan(planIdx, requestDto);

        return sendResponseHttpByJson(SUCCESS, "Update Plan. PLAN_INDEX=" + planIdx, responseDto);
    }

    /**
     * 내 계획 완수 여부 수정하는 API (완료)
     *
     */
   @ResponseBody
   @PatchMapping("/complete/{planIdx}")
    public ResponseEntity<ResponseApiMessage> updatePlanIsComplete(@PathVariable Long planIdx, @RequestBody PlanIsCompleteRequestDto requestDto) {
        PlanResponseDto responseDto = planService.updatePlanIsComplete(planIdx, requestDto);
        if(responseDto.getStatus().equals(UPDATE_PLAN_REDUNDANT_REQUEST)){
            return sendResponseHttpByJson(UPDATE_PLAN_REDUNDANT_REQUEST, "중복된 요청입니다. (이미"+ requestDto.getIsComplete() +")",requestDto);
        }

        return sendResponseHttpByJson(SUCCESS, "UPDATE IS COMPLETE PLAN. PLAN_INDEX=" + planIdx, responseDto);
    }


    /**
     * 내 계획 캘린더에 표시 여부 수정하는 API (완료)
     */
    @ResponseBody
    @PatchMapping("/isvisible/{planIdx}")
    public ResponseEntity<ResponseApiMessage> updatePlanIsVisible(@PathVariable Long planIdx) {
        PlanResponseDto responseDto = planService.updatePlanIsVisible(planIdx);
        if(responseDto.getStatus().equals(POST_PLAN_ISVISIBLE_FULL))
            return sendResponseHttpByJson(POST_PLAN_ISVISIBLE_FULL, "ISVISIBLE IS FULL. PLAN_INDEX=" + planIdx, responseDto);
        return sendResponseHttpByJson(SUCCESS, "UPDATE IS VISIBLE PLAN. PLAN_INDEX=" + planIdx, responseDto);
    }


    /**
     * 내 계획 삭제하는 API(완료)
     * @param planIdx: 삭제하려는 계획 인덱스
     */
    @ResponseBody
    @DeleteMapping("/{planIdx}")
    public ResponseEntity<ResponseApiMessage> deletePlan(@PathVariable Long planIdx){
        PlanResponseDto responseDto = planService.deletePlan(planIdx);

        return sendResponseHttpByJson(SUCCESS, "DELETE USER_STUDY_COMPLETE. USER_STUDY_INDEX=" + planIdx, null);
    }


}
