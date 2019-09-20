package com.im.demo.controller;

import com.im.demo.config.CodeTable;
import com.im.demo.config.Response;
import com.im.demo.server.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/login")
    public Response login(@RequestParam("name")String name){
        CopyOnWriteArraySet<WebSocketServer> webSocketSet = WebSocketServer.getWebSocketSet();
        List<WebSocketServer> collect = webSocketSet.stream().filter(WebSocketServer -> WebSocketServer.getName().equals(name)).collect(Collectors.toList());
        if(collect.size() > 0){
            return new Response(CodeTable.EXCEPTION,"该昵称已存在");
        }
        return new Response<>(WebSocketServer.getOnlineCount());
    }

}
