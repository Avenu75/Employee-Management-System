package com.rainbow.springboot.thymeleafdemo.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.Department;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeStatus;
import com.rainbow.springboot.thymeleafdemo.entity.Gender;
import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;
import com.rainbow.springboot.thymeleafdemo.exception.OtherWorkerNotFoundException;
import com.rainbow.springboot.thymeleafdemo.service.DepartmentService;
import com.rainbow.springboot.thymeleafdemo.service.OtherWorkerService;

@Controller
@RequestMapping("/other-workers")
public class OtherWorkerController {

    private final OtherWorkerService otherWorkerService;
    private final DepartmentService departmentService;

    public OtherWorkerController(OtherWorkerService theOtherWorkerService,
                                 DepartmentService theDepartmentService) {
        otherWorkerService = theOtherWorkerService;
        departmentService = theDepartmentService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String listWorkers(Model model) {
        List<OtherWorker> workers = otherWorkerService.findAll();
        model.addAttribute("workers", workers);
        return "other-workers/list-other-workers";
    }

    @GetMapping("/showFormForAdd")
    @PreAuthorize("hasAnyAuthority('CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String showFormForAdd(Model model) {
        OtherWorker worker = new OtherWorker();
        worker.setStatus(EmployeeStatus.ACTIVE);
        model.addAttribute("worker", worker);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("departments", departmentService.findAll());
        return "other-workers/other-worker-form";
    }

    @GetMapping("/showFormForUpdate")
    @PreAuthorize("hasAnyAuthority('CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String showFormForUpdate(@RequestParam("workerId") int id, Model model) {
        model.addAttribute("worker", otherWorkerService.findById(id));
        model.addAttribute("genders", Gender.values());
        model.addAttribute("departments", departmentService.findAll());
        return "other-workers/other-worker-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','SUPER_ADMIN')")
    public String saveWorker(@ModelAttribute("worker") OtherWorker worker,
                             @RequestParam(value = "profilePhoto", required = false) MultipartFile profileImage,
                             RedirectAttributes redirectAttributes) {

        if (profileImage != null && !profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please upload a valid image file.");
                return "redirect:/other-workers/showFormForAdd";
            }
            try {
                String base64 = Base64.getEncoder().encodeToString(profileImage.getBytes());
                worker.setProfileImage("data:" + contentType + ";base64," + base64);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image upload failed. Please try again.");
                return "redirect:/other-workers/showFormForAdd";
            }
        }

        if (worker.getStatus() == null) {
            worker.setStatus(EmployeeStatus.ACTIVE);
        }

        // Keep the stored photo when editing without uploading a new one.
        if (worker.getId() != 0 && (profileImage == null || profileImage.isEmpty())) {
            worker.setProfileImage(otherWorkerService.findById(worker.getId()).getProfileImage());
        }

        otherWorkerService.save(worker);
        redirectAttributes.addFlashAttribute("successMessage",
                "Other worker \"" + worker.getFullName() + "\" was saved successfully.");
        return "redirect:/other-workers/list";
    }

    @GetMapping("/profile/{workerId}")
    @PreAuthorize("hasAnyAuthority('CAN_VIEW_EMPLOYEE','CAN_ADD_EMPLOYEE','CAN_EDIT_EMPLOYEE','CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String viewWorkerProfile(@PathVariable int workerId,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            OtherWorker worker = otherWorkerService.findById(workerId);
            if (worker.getDepartment() != null) {
                worker.getDepartment().getName();
            }
            model.addAttribute("worker", worker);
            return "other-workers/other-worker-profile";
        } catch (OtherWorkerNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/other-workers/list";
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyAuthority('CAN_DELETE_EMPLOYEE','SUPER_ADMIN')")
    public String deleteWorker(@RequestParam("workerId") int id, RedirectAttributes redirectAttributes) {
        OtherWorker worker = otherWorkerService.findById(id);
        otherWorkerService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Other worker \"" + worker.getFullName() + "\" was deleted.");
        return "redirect:/other-workers/list";
    }
}
