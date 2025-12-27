package pl.komis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.komis.service.UserService;

@Controller
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserService userService;

    @GetMapping("/users")
    @ResponseBody
    public String checkUsers() {
        return userService.checkAllUsersPasswords();
    }

    @GetMapping("/fix-passwords")
    @ResponseBody
    public String fixPasswords() {
        userService.fixNonBCryptPasswords();
        return "Naprawiono hasła";
    }
}