package com.xinghuo.controller;

import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.web.AccountAdminApiController;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.service.XingweiOperationsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class XingweiOperationsController {
    private final XingweiOperationsService work;
    public XingweiOperationsController(XingweiOperationsService work){this.work=work;}
    @ModelAttribute void privateResponse(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/admin/xingwei/operations") String page(Model model,CsrfToken csrf,Authentication actor){var access=work.access(actor);@SuppressWarnings("unchecked") var modules=(Map<String,Map<String,Boolean>>)access.get("modules");if(modules.values().stream().noneMatch(value->Boolean.TRUE.equals(value.get("view"))))throw new AccessDeniedException("没有可访问的运营模块");model.addAttribute("csrf",csrf);return "admin-xingwei-operations";}
    @GetMapping("/api/admin/xingwei/operations/access") @ResponseBody Object access(Authentication actor){return work.access(actor);}
    @GetMapping("/api/admin/xingwei/operations/members") @ResponseBody Object members(@RequestParam(required=false) String q,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Authentication actor){return work.members(q,status,page,actor);}
    @GetMapping("/api/admin/xingwei/operations/members/{id}") @ResponseBody ResponseEntity<?> member(@PathVariable long id,Authentication actor){return state(work.member(id,actor));}
    @PostMapping("/api/admin/xingwei/operations/members") @ResponseBody ResponseEntity<?> createMember(@Valid @RequestBody AccountAdminApiController.UserCreateRequest body,Authentication actor,HttpServletRequest request){return state(work.createMember(body,actor,request));}
    @PatchMapping("/api/admin/xingwei/operations/members/{id}") @ResponseBody ResponseEntity<?> updateMember(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.UserUpdateRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.updateMember(id,body,revision,actor,request));}
    @PostMapping("/api/admin/xingwei/operations/members/{id}/verified") @ResponseBody ResponseEntity<?> verify(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.EmailVerifiedRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.verifyMember(id,body,revision,actor,request));}
    @PostMapping("/api/admin/xingwei/operations/members/{id}/password") @ResponseBody ResponseEntity<?> password(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.PasswordResetRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.resetMemberPassword(id,body,revision,actor,request));}
    @DeleteMapping("/api/admin/xingwei/operations/members/{id}") @ResponseBody ResponseEntity<?> delete(@PathVariable long id,@RequestBody Confirmation body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){work.deleteMember(id,body.confirmation(),revision,actor,request);return ResponseEntity.noContent().build();}
    @GetMapping("/api/admin/xingwei/operations/members/{id}/logs") @ResponseBody Object logs(@PathVariable long id,Authentication actor){return work.loginLogs(id,actor);}
    @GetMapping("/api/admin/xingwei/operations/comments") @ResponseBody Object comments(@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Authentication actor){return work.comments(status,page,actor);}
    @PostMapping("/api/admin/xingwei/operations/comments/{id}/review") @ResponseBody ResponseEntity<?> comment(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.ReviewRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.reviewComment(id,body,revision,actor,request));}
    @DeleteMapping("/api/admin/xingwei/operations/comments/{id}") @ResponseBody ResponseEntity<?> deleteComment(@PathVariable long id,@RequestParam(required=false) String note,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){work.deleteComment(id,note,revision,actor,request);return ResponseEntity.noContent().build();}
    @GetMapping("/api/admin/xingwei/operations/submissions") @ResponseBody Object submissions(@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Authentication actor){return work.submissions(status,page,actor);}
    @GetMapping("/api/admin/xingwei/operations/submissions/{id}") @ResponseBody ResponseEntity<?> submission(@PathVariable long id,Authentication actor){return state(work.submission(id,actor));}
    @PostMapping("/api/admin/xingwei/operations/submissions/{id}/review") @ResponseBody ResponseEntity<?> reviewSubmission(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.SubmissionReviewRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.reviewSubmission(id,body,revision,actor,request));}
    @PostMapping("/api/admin/xingwei/operations/submissions/{id}/publish") @ResponseBody ResponseEntity<?> publish(@PathVariable long id,@Valid @RequestBody AccountAdminApiController.PublishRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.publishSubmission(id,body,revision,actor,request));}
    @GetMapping("/api/admin/xingwei/operations/feedback") @ResponseBody Object feedback(@RequestParam(required=false) String q,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Authentication actor){return work.feedback(q,status,page,actor);}
    @PostMapping("/api/admin/xingwei/operations/feedback") @ResponseBody ResponseEntity<?> createFeedback(@Valid @RequestBody ApiDtos.TestFeedbackCreateRequest body,Authentication actor,HttpServletRequest request){return state(work.createFeedback(body,actor,request));}
    @PatchMapping("/api/admin/xingwei/operations/feedback/{id}") @ResponseBody ResponseEntity<?> updateFeedback(@PathVariable long id,@Valid @RequestBody ApiDtos.TestFeedbackUpdateRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.updateFeedback(id,body,revision,actor,request));}
    @PostMapping("/api/admin/xingwei/operations/feedback/{id}/attachment") @ResponseBody ResponseEntity<?> upload(@PathVariable long id,@RequestParam MultipartFile file,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.feedbackImage(id,file,revision,actor,request));}
    @GetMapping("/api/admin/xingwei/operations/feedback/{id}/attachment") @ResponseBody ResponseEntity<Resource> attachment(@PathVariable long id,Authentication actor){return work.feedbackImage(id,actor);}
    @GetMapping("/api/admin/xingwei/operations/display") @ResponseBody Object display(Authentication actor){return work.display(actor);}
    @GetMapping("/api/admin/xingwei/operations/display/services") @ResponseBody Object services(Authentication actor){return work.displayServices(actor);}
    @PatchMapping("/api/admin/xingwei/operations/display") @ResponseBody ResponseEntity<?> updateDisplay(@Valid @RequestBody ApiDtos.SlotUpdate body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.updateDisplay(body,revision,actor,request));}
    @GetMapping("/api/admin/xingwei/operations/leads") @ResponseBody Object leads(@RequestParam(required=false) String q,@RequestParam(required=false) String status,@RequestParam(required=false) String intent,@RequestParam(required=false) String source,@RequestParam(defaultValue="0") int page,Authentication actor){return work.leads(q,status,intent,source,page,actor);}
    @GetMapping("/api/admin/xingwei/operations/leads/{no}") @ResponseBody ResponseEntity<?> lead(@PathVariable String no,Authentication actor){return state(work.lead(no,actor));}
    @PatchMapping("/api/admin/xingwei/operations/leads/{no}") @ResponseBody ResponseEntity<?> updateLead(@PathVariable String no,@Valid @RequestBody QuoteLeadDtos.QuoteCrmUpdateRequest body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.updateLead(no,body,revision,false,actor,request));}
    @PostMapping("/api/admin/xingwei/operations/leads/{no}/suggestion") @ResponseBody ResponseEntity<?> suggestion(@PathVariable String no,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){return state(work.updateLead(no,null,revision,true,actor,request));}
    private ResponseEntity<?> state(XingweiOperationsService.State state){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(state.revision()).body(state);}
    @ExceptionHandler(AccountException.class) @ResponseBody ResponseEntity<?> accountError(AccountException e){return ResponseEntity.status(e.getStatus()).body(Map.of("error",e.getCode(),"message",e.getMessage()));}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody ResponseEntity<?> conflict(OptimisticLockingFailureException e){return ResponseEntity.status(409).body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) @ResponseBody ResponseEntity<?> denied(AccessDeniedException e){return ResponseEntity.status(403).body(Map.of("message",e.getMessage()));}
    public record Confirmation(String confirmation){}
}
