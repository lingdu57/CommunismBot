package com.aye10032;

import com.aye10032.NLP.DataCollect;
import com.aye10032.functions.*;
import com.aye10032.functions.funcutil.IFunc;
import com.aye10032.functions.funcutil.SimpleMsg;
import com.aye10032.timetask.DragraliaTask;
import com.aye10032.timetask.SimpleSubscription;
import com.aye10032.utils.ExceptionUtils;
import com.aye10032.utils.IMsgUpload;
import com.aye10032.utils.SeleniumUtils;
import com.aye10032.utils.StringUtil;
import com.aye10032.utils.timeutil.ITimeAdapter;
import com.aye10032.utils.timeutil.SubscriptManager;
import com.aye10032.utils.timeutil.TimeTaskPool;
import com.dazo66.message.MiraiSerializationKt;
import com.firespoon.bot.command.Command;
import com.firespoon.bot.commandbody.CommandBody;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.MemberMuteEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.message.MessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.MiraiLogger;
import net.mamoe.mirai.utils.PlatformLogger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//msg = msg.replace("&#91;", "[").replace("&#93;", "]");

/**
 * @author Dazo66
 */
public class Zibenbot {

    public static Proxy proxy = null;
    public static MiraiLogger logger;
    private static Pattern AT_REGEX = Pattern.compile("\\[mirai:at:(\\d+),[\\S|\\s]+]");
    private static Function2<? super CommandBody<MessageEvent>, ? super Continuation<? super Unit>, ?> msgAction = (o, o2) -> Unit.INSTANCE;
    //时间任务池
    public TimeTaskPool pool;
    public SubscriptManager subManager = new SubscriptManager(this);
    //public TeamspeakBot teamspeakBot;
    public BotConfigFunc config;
    public FuncEnableFunc enableCollFunc;
    public List<Long> enableGroup = new ArrayList<>();
    public String appDirectory;
    List<IFunc> registerFunc = new ArrayList<>();
    private Bot bot;
    private Function2<Object, Object, ?>
            msgBuilder = (o, o2) -> {
        if (o instanceof MessageEvent) {
            SimpleMsg simpleMsg = new SimpleMsg((MessageEvent) o);
            if (simpleMsg.isGroupMsg() && !enableGroup.contains(simpleMsg.getFromGroup())) {
                return null;
            } else {
                runFuncs(simpleMsg);
            }
        }
        return null;
    };
    private Command<MessageEvent> command = new ZibenbotController("Zibenbot", msgBuilder, msgAction);
    //private Map<String, Image> miraiImageMap = new ConcurrentHashMap<>();
    private Map<Integer, File> imageMap = new ConcurrentHashMap<>();

    {

        //fromGroup == 995497677L
        // || fromGroup == 792666782L
        // || fromGroup == 517709950L
        // || fromGroup == 295904863
        // || fromGroup == 947657871
        // || fromGroup == 456919710L
        // || fromGroup == 792797914L
        enableGroup.add(995497677L); //提醒人
        enableGroup.add(792666782L); //实验室
        enableGroup.add(517709950L); //植物群
        enableGroup.add(295904863L); //魔方社
        enableGroup.add(947657871L); //TIS内群
        enableGroup.add(456919710L); //红石科技搬运组
        enableGroup.add(792797914L); //TIS Lab
        enableGroup.add(814843368L); //dazo群
        enableGroup.add(1107287775L); //Test
        enableGroup.add(980042772L); //公会
        appDirectory = "data";
        SeleniumUtils.setup(appDirectory + "\\ChromeDriver\\chromedriver.exe");
    }

    private PrintStream LOGGER_FILE = null;

    private synchronized PrintStream getLoggerStream() {
        if (LOGGER_FILE == null) {
            File logDir = new File(appDirectory + "\\log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            File file = new File(appDirectory + "\\log\\log-" + format.format(new Date()) + ".log");
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                LOGGER_FILE = new PrintStream(new FileOutputStream(file, true), true);
                LOGGER_FILE.println("------------------------------------------------------------------");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return LOGGER_FILE;

    }

    public Zibenbot(Bot bot) {
        this.bot = bot;
        logger = new PlatformLogger("zibenbot", (String s) -> {
            System.out.println(s);
            getLoggerStream().println(s);
            return Unit.INSTANCE;
        }, true);
        bot.getLogger().plus(logger);
        pool = new TimeTaskPool();

    }

    public static Proxy getProxy() {
        Socket s = new Socket();
        SocketAddress add = new InetSocketAddress("127.0.0.1", 1080);
        try {
            s.connect(add, 1000);
            proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 1080));
        } catch (IOException e) {
            //连接超时需要处理的业务逻辑
        }
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return proxy;
    }

    public Command<MessageEvent> getCommand() {
        return command;
    }

    public long getAtMember(String s) {
        List<Long> list = getAtMembers(s);
        if (list.size() != 0) {
            return list.get(0);
        } else {
            return -1;
        }
    }

    public List<Long> getAtMembers(String s) {
        List<Long> rets = new ArrayList<>();
        try {
            Matcher matcher = AT_REGEX.matcher(s);
            int i = 0;
            while (matcher.find(i)) {
                rets.add(Long.parseLong(matcher.group(1)));
                i = matcher.start() + 1;
            }
            return rets;
        } catch (Exception e) {
            System.out.println(rets);
            return rets;
        }
    }

    private void muteMember(Member member, int second) {
        member.mute(second);
    }

    public void muteMember(long groupId, long memberId, int second) {

        Member member = null;
        try {
            member = _getGroup(groupId).get(memberId);
        } catch (Exception e) {
            logWarning("禁言失败：" + memberId + e);
            return;
        }

        muteMember(member, second);
    }

    public void setMuteAll(long groupId, boolean muteAll) {
        Group g = _getGroup(groupId);
        if (g == null) {
            logWarning("全体禁言失败，找不到group：" + groupId);
            return;
        }
        g.getSettings().setMuteAll(muteAll);
    }

    private void unMute(Member member) {
        member.unmute();
    }

    public void unMute(long groupId, long memberId) {
        Member member = null;
        try {
            member = _getGroup(groupId).get(memberId);
        } catch (Exception e) {
            logWarning("禁言失败：" + memberId + e);
            return;
        }
        unMute(member);
    }

    public void onMute(MemberMuteEvent event){
        setMuteTimeLocal(event.getMember(), event.getDurationSeconds());
    }

    private void setMuteTimeLocal(Member member, int time){
        try {
            Field field = member.getClass().getDeclaredField("_muteTimestamp");
            field.setAccessible(true);
            field.set(member, new Long(System.currentTimeMillis() / 1000).intValue() + time);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 推荐使用这个方法进行at
     * 不是每个CqMsg都有Event
     *
     * @param clientId id
     * @return at MiraiCode
     */
    public String at(long clientId) {
        User user = findUser(clientId);
        if (user == null) {
            return String.valueOf(clientId);
        } else {
            return at(user);
        }
    }

    private String at(User user) {
        if (user == null) {
            return "null";
        }
        String displayName = user.getNick();
        long id = user.getId();
        At at = null;
        if (user instanceof net.mamoe.mirai.contact.Member) {
            at = new At((net.mamoe.mirai.contact.Member) user);
        } else {
            try {
                Constructor c1 = At.class.getDeclaredConstructor(long.class, String.class);
                c1.setAccessible(true);
                at = (At) c1.newInstance(id, displayName);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        }
        if (at == null) {
            return "@" + displayName;
        } else {
            return at.toMiraiCode();
        }
    }

    public List<IFunc> getRegisterFunc() {
        return registerFunc;
    }

    public void toPrivateMsg(long clientId, String msg) {
        toPrivateMsg(clientId, toMessChain(findUser(clientId), msg));
    }

    private void toPrivateMsg(long clientId, MessageChain chain, boolean flag) {
        Contact contact = findUser(clientId);
        if (contact == null) {
            logWarning("找不到Contact：" + clientId);
            return;
        }
        if (!flag) {
            contact.sendMessage(chain);
            return;
        }
        try {
            contact.sendMessage(chain);
        } catch (IllegalStateException e) {
            String s = e.getMessage();
            if (s.contains("resultType=10")) {
                if (contact instanceof Member) {
                    contact.sendMessage("发送消息失败，可能需要添加好友。");
                    return;
                }
                contact.sendMessage("发送消息失败，消息过长，将分段发送。");
                longMsgSplit(chain, 250).forEach(c -> toPrivateMsg(clientId, c, false));
            } else if (s.contains("resultType=32")) {
                contact.sendMessage("发送消息失败，请尝试添加好友再获取。");
            } else {
                logWarning(ExceptionUtils.printStack(e));
            }
        }
    }

    public void toPrivateMsg(long clientId, MessageChain chain) {
        toPrivateMsg(clientId, chain, true);
    }

/*    public int toTeamspeakMsg(String msg) {
        teamspeakBot.api.sendChannelMessage(msg);
        return 1;
    }*/

    private User findUser(long clientId) {
        try {
            return bot.getFriend(clientId);
        } catch (NoSuchElementException e) {
            for (Group group : bot.getGroups()) {
                try {
                    return group.get(clientId);
                } catch (NoSuchElementException ignored) {
                }
            }
        }
        return null;
    }

    public void toGroupMsg(long groupId, String msg) {
        Group group = _getGroup(groupId);
        if (group == null) {
            logWarning("找不到Group：" + groupId);
            return;
        }
        group.sendMessage(toMessChain(group, msg));
    }

    public void log(Level level, String msg) {
        if (level == Level.WARNING) {
            logWarning(msg);
        } else if (level == Level.ALL) {
                logVerbose(msg);
        } else {
            logInfo(msg);
        }

    }

    public void logInfo(String info) {
        logger.info(info);
    }

    public void logDebug(String debugMsg) {
        logger.debug(debugMsg);
    }

    public void logError(String errorMsg) {
        logger.error(errorMsg);
    }

    public void logWarning(String warnMsg){
        logger.warning(warnMsg);
    }

    public void logVerbose(String verboseMsg){
        logger.verbose(verboseMsg);
    }

    public void replyMsg(SimpleMsg fromMsg, String msg) {
        try {
            if (fromMsg.isGroupMsg()) {
                Contact contact = _getGroup(fromMsg.getFromGroup());
                if (contact != null) {
                    MessageChain chain = toMessChain(contact, msg);
                    contact.sendMessage(chain);
                }
            } else if (fromMsg.isPrivateMsg()) {
                    MessageChain chain = toMessChain(findUser(fromMsg.getFromClient()), msg);
                    toPrivateMsg(fromMsg.getFromClient(), chain);
            } else if (fromMsg.isTeamspealMsg()) {
/*            Zibenbot.logger.log(Level.INFO,
                    String.format("回复ts频道[%s]消息:%s",
                            fromMsg.fromGroup,
                            msg));*/
                //todo
            }
        } catch (Exception e) {
            logWarning(ExceptionUtils.printStack(e));
        }
    }

    private static List<String> split(String s, int length){
        List<String> ret = new ArrayList<>();
        int size = s.length() / length + 1;
        for (int i = 0; i < size ;i++) {
            int i1 = (i + 1) * length;
            if (i1 > s.length()) {
                i1 = s.length();
            }
            ret.add(s.substring(i * length, i1));
        }
        return ret;
    }

    private static List<MessageChain> longMsgSplit(MessageChain chain, final int MAX_LENGTH) {
        List<Message> list = new ArrayList<>();

        for (Message message : chain) {
            if (message.toString().length() >= MAX_LENGTH) {
                if (message instanceof PlainText) {
                    String s = message.toString();
                    String[] strings = s.split("\n");
                    for (String s2 : strings) {
                        if (s2.length() > MAX_LENGTH) {
                            split(s2, MAX_LENGTH).forEach(s1 -> list.add(new PlainText(s1)));
                            list.add(new PlainText("\n"));
                        } else {
                            list.add(new PlainText(s2));
                            list.add(new PlainText("\n"));
                        }
                    }
                    list.remove(list.size() - 1);
                }
            } else {
                list.add(message);
            }
        }
        List<MessageChain> ret = new ArrayList<>();
        int i = 0;
        MessageChainBuilder builder = new MessageChainBuilder();
        for (Message message : list) {
            String s = message.toString();
            if (i + s.length() >= MAX_LENGTH) {
                ret.add(builder.build());
                builder = new MessageChainBuilder();
                builder.add(message);
                i = s.length();
            } else {
                builder.add(message);
                i += s.length();
            }
        }
        //把剩余的加进去
        if (builder.size() != 0) {
            ret.add(builder.build());
        }
        return ret;
    }

    public void onFriendEvent(NewFriendRequestEvent event){
        if (findUser(event.getFromId()) != null) {
            event.accept();
        }
    }

    final Map<String, IMsgUpload> msgUploads = new HashMap<>();
    final Pattern MSG_TYPE_PATTERN = Pattern.compile(String.format("\\[type=(%s),[ ]*source=\"([[^\"\\f\\n\\r\\t\\v]]+)\"]"
            , StringUtil.splicing("|", msgUploads.keySet())));

    {
        msgUploads.put("IMAGE", (conect, source) -> conect.uploadImage(new File(source)).toMiraiCode());
        msgUploads.put("VOICE", (conect, source) -> {
            if (conect instanceof Group) {
                return ((Group) conect).uploadVoice(new FileInputStream(source)).toString();
            } else {
                return "[VOICE]";
            }
        });
        msgUploads.put("AT", (conect, source) -> {
            if (conect instanceof User) {
                return at(Long.valueOf(source));
            } else {
                return "@" + source;
            }
        });
    }

    private MessageChain toMessChain(Contact send, String msg) {
        String s = replaceMsgType(send, msg);
        /*if (send instanceof Friend) {
            String fromto = String.valueOf(bot.getId()) + "-" + String.valueOf(send.getId());
            s = s.replaceAll("\\[mirai:image:\\{(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})}.mirai]", "[mirai:image:/" + fromto + "-$1$2$3$4$5"+"]");
        } else {
            s = s.replaceAll("\\[mirai:image:/(\\d+)-(\\d+)-(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})]", "[mirai:image:{$3-$4-$5-$6-$7}.mirai]");
        }*/
        return MiraiSerializationKt.parseMiraiCode(s);
    }

    public int startup() {
        SeleniumUtils.setup(appDirectory + "\\ChromeDriver\\chromedriver.exe");
        ITimeAdapter maiyaoCycle = date1 -> {
            Calendar c = Calendar.getInstance();
            c.setTime(date1);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (0 <= hour && hour < 6) {
                c.set(Calendar.HOUR_OF_DAY, 6);
            } else if (6 <= hour && hour < 12) {
                c.set(Calendar.HOUR_OF_DAY, 12);
            } else if (12 <= hour && hour < 18) {
                c.set(Calendar.HOUR_OF_DAY, 18);
            } else {
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.setTime(new Date(c.getTimeInMillis() + 86400 * 1000));
            }

            return c.getTime();
        };
        ITimeAdapter jiaomieCycle = date -> {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            if (1 == dayOfWeek) {
                if (hour < 22) {
                } else {
                    c.set(Calendar.DAY_OF_WEEK, 2);
                }
                c.set(Calendar.HOUR_OF_DAY, 22);

            } else if (2 == dayOfWeek) {
                if (hour < 22) {
                } else {
                    c.set(Calendar.WEEK_OF_YEAR, c.get(Calendar.WEEK_OF_YEAR) + 1);
                    c.set(Calendar.DAY_OF_WEEK, 1);
                }
                c.set(Calendar.HOUR_OF_DAY, 22);
            } else {
                c.set(Calendar.WEEK_OF_YEAR, c.get(Calendar.WEEK_OF_YEAR) + 1);
                c.set(Calendar.DAY_OF_WEEK, 1);
                c.set(Calendar.HOUR_OF_DAY, 22);
            }
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        };

        ITimeAdapter dakaCycle = date2 -> {
            Calendar c = Calendar.getInstance();
            c.setTime(date2);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);
            if (0 <= hour && hour < 8 && min < 30) {
                c.set(Calendar.HOUR_OF_DAY, 8);
                c.set(Calendar.MINUTE,30);
            } else {
                c.setTime(new Date(c.getTimeInMillis() + 86400 * 1000));
            }

            return c.getTime();
        };


        logInfo("registe time task start");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        /*subManager.addSubscribable(new SimpleSubscription(this, TimeConstant.NEXT_MIN, () -> new Date().toString()) {
            @Override
            public String getName() {
                return "test";
            }
        });*/

        //创建订阅器对象
        SimpleSubscription maiyao = new SimpleSubscription(this, maiyaoCycle,
                getImg(appDirectory + "/image/提醒买药小助手.jpg")) {
            private final static String NAME = "提醒买药小助手";

            @Override
            public String getName() {
                return NAME;
            }
        };
        SimpleSubscription jiaomie = new SimpleSubscription(this, jiaomieCycle,
                getImg(appDirectory + "/image/提醒剿灭小助手.jpg")) {
            @Override
            public String getName() {
                return "提醒剿灭小助手";
            }
        };
        subManager.setTiggerTime(date);
        subManager.addSubscribable(maiyao);
        subManager.addSubscribable(jiaomie);
        subManager.addSubscribable(new DragraliaTask(this) {
            private final static String NAME = "龙约公告转发小助手";

            @Override
            public String getName() {
                return NAME;
            }
        });
        //把订阅管理器注册进线程池
        pool.add(subManager);
        //把订阅管理器注册进可用的模块里
        registerFunc.add(subManager);

        logInfo("registe time task end");
        //改成了手动注册
        log(Level.INFO, "registe func start");

        registerFunc.add(config = new BotConfigFunc(this));
        registerFunc.add(enableCollFunc = new FuncEnableFunc(this));
        registerFunc.add(new CubeFunc(this));
        registerFunc.add(new BanFunc(this));
        registerFunc.add(new DianGuaiFunc(this));
        registerFunc.add(new EatFunc(this));
        registerFunc.add(new FangZhouDiaoluoFunc(this));
        registerFunc.add(new liantongFunc(this));
        registerFunc.add(new nmslFunc(this));
        registerFunc.add(new PixivFunc(this));
        registerFunc.add(new BiliFunc(this));
        registerFunc.add(new RedStoneFunc(this));
        registerFunc.add(new ScreenshotFunc(this));
        registerFunc.add(new DragraliaNewsFunc(this));
        registerFunc.add(new DraSummonSimulatorFunc(this));
        registerFunc.add(new PaomianFunc(this));
        registerFunc.add(new SendGroupFunc(this));
        registerFunc.add(new INMFunc(this));
        registerFunc.add(new DataCollect(this));
        registerFunc.add(new CheruFunc(this));
        registerFunc.add(new QueueFunc(this));
        //对功能进行初始化
        for (IFunc func : registerFunc) {
            try {
                func.setUp();
            } catch (Exception e) {
                logWarning("初始化：" + func.getClass().getName() + "出现异常");
            }
        }
        log(Level.INFO, "registe func end");

/*        //创建teamspeakbot对象
        teamspeakBot = new TeamspeakBot(this);
        try {
            teamspeakBot.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return 0;
    }

    /**
     * 根据groupid返回group对象
     * @param id groupid
     * @return 返回group对象 找不到时返回null
     */
    private Group _getGroup(long id) {
        try {
            return bot.getGroup(id);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public List<Long> getGroups() {
        List<Long> list = new ArrayList<>();
        bot.getGroups().forEach(group -> list.add(group.getId()));
        return list;
    }

    public List<Long> getFriends() {
        List<Long> list = new ArrayList<>();
        bot.getFriends().forEach(friend -> list.add(friend.getId()));
        return list;
    }

    public List<Long> getMembers(long groupId) {
        List<Long> list = new ArrayList<>();
        Group group = _getGroup(groupId);
        if (group != null) {
            group.getMembers().forEach(member -> list.add(member.getId()));
        }
        return list;
    }

    public String getUserName(long userId){
        User user = findUser(userId);
        if (user != null) {
            return user.getNick();
        }
        return "null";
    }

    public int getMuteTimeRemaining(long groupId, long memberId) {
        Group group = _getGroup(groupId);
        try {
            Member member = group.get(memberId);
            return member.getMuteTimeRemaining();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 根据文件路径返回声音字符串
     *
     * @param path 路径
     * @return 声音字符串
     */
    public String getVoice(String path) {
        return getMsg("VOICE", path);
    }

    /**
     * 根据文件路径返回图片字符串
     *
     * @param path 文件路径
     * @return 图片字符串
     */
    public String getImg(String path) {
        return getMsg("IMAGE", path);
    }

    /**
     * 根据文件返回图片字符串
     *
     * @param file 文件
     * @return 图片字符串
     */
    public String getImg(File file) {
        return getMsg("IMAGE", file.getAbsolutePath());
    }

    public String getMsg(String type, String source) {
        return String.format("[type=%s, source=\"%s\"]", type, source);
    }

    private String replaceMsgType(Contact contact, String msg) {
        Matcher matcher = MSG_TYPE_PATTERN.matcher(msg);
        int i = 0;
        while (matcher.find(i)) {
            msg = msg.replace(matcher.group(0), _upload(contact, matcher.group(1), matcher.group(2)));
            i = matcher.start() + 1;
        }
        return msg;
    }

    private String _upload(Contact contact, String type, String source) {
        try {
            return msgUploads.get(type).upload(contact, source);
        } catch (Exception e) {
            logWarning(String.format("上传%s失败：%s", type, ExceptionUtils.printStack(e)));
        }
        return "[" + type + "]";
    }

    private String toMiraiImage(Contact contact, String msg){
        //[mirai:image:{FE417B3B-F6F2-7BA0-3F2D-1FEF5DB15E4E}.mirai]
        //[mirai:image:/895981998-3405930276-FE417B3BF6F27BA03F2D1FEF5DB15E4E]
        for (Map.Entry<Integer, File> entry : imageMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (msg.contains(key)) {
                String img = "[图片]";
                if (contact != null) {
                    Image image;
                    image = contact.uploadImage(entry.getValue());
                    //miraiImageMap.put(image.getImageId(), image);
                    img = image.toMiraiCode();
                }
                msg = msg.replace(key, img);
            }
        }
        return msg;
    }

    public void runFuncs(SimpleMsg simpleMsg) {

        for (IFunc func : registerFunc) {
            if (enableCollFunc.isEnable(simpleMsg.getFromGroup(), func)) {
                try {
                    func.run(simpleMsg);
                } catch (Exception e) {
                    replyMsg(simpleMsg, "运行出错：" + e + "\n" + ExceptionUtils.printStack(e));
                }
            }
        }

    }

/*    public int teamspeakMsg(long fromGroup, long fromClient, String msg) {
        // 如果消息来自匿名者
        SimpleMsg cqMsg = new SimpleMsg(-1, -1, fromGroup, fromClient, null, msg, -1, MsgType.TEAMSPEAK_MSG);
        for (IFunc func : registerFunc) {
            try {
                func.run(cqMsg);
            } catch (Exception e) {
                replyMsg(cqMsg, "运行出错：" + e + "\n" + ExceptionUtils.printStack(e));
            }
        }
        return MSG_IGNORE;
    }*/

    static class ZibenbotController extends Command<MessageEvent> {
        public ZibenbotController(@NotNull String name, @NotNull Function2<? super MessageEvent, ? super Continuation<? super CommandBody<MessageEvent>>, ?> builder, @NotNull Function2<? super CommandBody<MessageEvent>, ? super Continuation<? super Unit>, ?> action) {
            super(Listener.EventPriority.NORMAL, name, builder, action);
        }
    }

}
