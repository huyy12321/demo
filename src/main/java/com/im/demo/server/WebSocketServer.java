package com.im.demo.server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;


/**
 * @author hyy
 */
@ServerEndpoint(value = "/websocket/{name}")
@Component
@Slf4j
public class WebSocketServer {
    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;

    /**
     *  concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    /**
     * 等待匹配中的用户
     */
    private static CopyOnWriteArraySet<WebSocketServer> waitUser = new CopyOnWriteArraySet<WebSocketServer>();
    /**
     *  与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 进行中的游戏
     */
    private static ConcurrentHashMap<String,Object> games = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private String name;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session, @PathParam("name")String name) {
        this.session = session;
        this.name = name;
        // 加入set中
        webSocketSet.add(this);
        // 在线数加1
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage("欢迎新人"+name+"加入");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        // 从set中删除
        webSocketSet.remove(this);
        // 在线数减1
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject jsonObject = (JSONObject) JSONObject.parse(message);
        Integer type = (Integer) jsonObject.get("type");
        switch (type){
            case 1:
                start(this);
                break;
            case 2:
                String gameId = jsonObject.get("gameId").toString();
                Integer start = (Integer)jsonObject.get("start");
                Integer x = (Integer) jsonObject.get("x");
                Integer y = (Integer) jsonObject.get("y");
                click(gameId,start,x,y);
                break;
            default:
                System.out.println("default");
                break;
        }
    }


    private void start(WebSocketServer webSocketServer) throws Exception{
        if(waitUser.size() > 0) {
            for(WebSocketServer user : waitUser) {
                if(!user.equals(webSocketServer)){
                    Map<String, Object> map = new HashMap<>();
                    String uuid = UUID.randomUUID().toString();
                    map.put("type",1);
                    map.put("code",0);
                    map.put("gameId",uuid);
                    map.put("msg",user.getName());
                    map.put("start",-1);
                    webSocketServer.sendMessage(map);
                    map.put("msg",webSocketServer.getName());
                    map.put("start",1);
                    user.sendMessage(map);
                    waitUser.remove(user);
                    HashMap<Object, Object> game = new HashMap<>();
                    game.put("user1",webSocketServer);
                    game.put("user2",user);
                    game.put("game",new int[20][20]);
                    games.put(uuid,game);
                }
            }
        } else{
            waitUser.add(webSocketServer);
        }
    }

    private void click(String gameId,Integer start,Integer x,Integer y) throws IOException {
        Map<String,Object> o = (Map<String, Object>) games.get(gameId);
        int[][] game = (int[][]) o.get("game");
        WebSocketServer user1 = (WebSocketServer) o.get("user1");
        WebSocketServer user2 = (WebSocketServer) o.get("user2");
        game[x][y] = start;
        boolean win = WebSocketServer.isWin(game, x, y, start);
        Map<String,Object> map = new HashMap<>();
        map.put("type",2);
        map.put("x",x);
        map.put("win",win);
        map.put("y",y);
        map.put("start",start);
        user1.sendMessage(map);
        user2.sendMessage(map);
    }

    public static void main(String[] args) {
       int[][] a = {{1,1,1,1,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0}};
       for (int[] b : a) {
           System.out.println(Arrays.toString(b));
       }
        System.out.println(WebSocketServer.isWin(a,4,0,1));
    }

    public static boolean isWin(int[][] game,int x,int y,int flag) {
        int rs = game[y][x];
        int m = x;
        int n = y;
        int length = 1;
        if(rs != 0) {
            return false;
        }
        while (--m >= 0 && game[y][m] == flag) {
            length++;
        }
        m = x;
        while (++m < 15 && game[y][m] == flag) {
            length++;
        }
        if(length > 4) {
            return true;
        }
        length = 1;
        while (--n >= 0 && game[n][x] == flag) {
            length++;
        }
        n = y;
        while (++n < 15 && game[n][x] == flag) {
            length++;
        }
        if(length > 4) {
            return true;
        }
        m = x;
        n = y;
        length = 1;
        while (++m < 15 && ++n < 15 &&  game[n][m] == flag) {
            length++;
        }
        m = x;
        n = y;
        while (--m >= 0 && --n >= 0 &&  game[n][m] == flag) {
            length++;
        }
        if(length > 4) {
            return true;
        }
        m = x;
        n = y;
        length = 1;
        while (--m < 15 && ++n < 15 &&  game[n][m] == flag) {
            length++;
        }
        m = x;
        n = y;
        while (++m >= 0 && --n >= 0 &&  game[n][m] == flag) {
            length++;
        }
        if(length > 4) {
            return true;
        }
        return false;
    }

    /**
     * 发生错误时调用
     * */
     @OnError
     public void onError(Session session, Throwable error) {
         System.out.println("发生错误");
         error.printStackTrace();
     }


      private void sendMessage(String message) throws IOException {
         //this.session.getBasicRemote().sendText(message); // 同步发送消息
          Map<String,Object> map = new HashMap<>();
          map.put("message",message);
          map.put("number",WebSocketServer.getOnlineCount());
          List<String> collect = webSocketSet.stream().map(WebSocketServer::getName).collect(Collectors.toList());
          map.put("userName",collect);
          // 异步发送消息
          this.session.getAsyncRemote().sendText(JSON.toJSONString(map));
     }
    private void sendMessage(Map<String,Object> map) throws IOException {
        // 异步发送消息
        this.session.getAsyncRemote().sendText(JSON.toJSONString(map));
    }


     /**
      * 群发自定义消息
      * */
    public static void sendInfo(String message) throws IOException {
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                log.error("群发消息出现异常：{}",e.toString());
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    private static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    private static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    public static CopyOnWriteArraySet<WebSocketServer> getWebSocketSet(){
        return webSocketSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketServer that = (WebSocketServer) o;
        return Objects.equals(session, that.session) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, name);
    }
}