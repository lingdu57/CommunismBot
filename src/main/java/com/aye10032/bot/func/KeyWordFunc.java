package com.aye10032.bot.func;

import com.aye10032.bot.Zibenbot;
import com.aye10032.bot.func.funcutil.BaseFunc;
import com.aye10032.bot.func.funcutil.FuncExceptionHandler;
import com.aye10032.bot.func.funcutil.SimpleMsg;
import com.aye10032.foundation.utils.command.Commander;
import com.aye10032.foundation.utils.command.CommanderBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Random;

/**
 * @author Dazo66
 */
@Service
public class KeyWordFunc extends BaseFunc {

    private Random random;
    private Commander<SimpleMsg> commander;

    public KeyWordFunc(Zibenbot zibenbot) {
        super(zibenbot);
        random = new Random(System.currentTimeMillis());
        commander = new CommanderBuilder<SimpleMsg>()
                .seteHandler(FuncExceptionHandler.INSTENCE)
                .start()
                .or("nmsl"::equalsIgnoreCase)
                .run((msg) -> {
                    zibenbot.replyMsg(msg, zibenbot.getImg(new File(appDirectory + "/image/dragon.jpg"))
                            + " 疯牛满地跑，难免输了");
                })
                .or("炼铜"::contains)
                .run((msg) -> {
                    if (random.nextDouble() < 0.4d) {
                        zibenbot.replyMsg(msg, zibenbot.getImg(new File(appDirectory + "/image/liantong.jpg")));
                    }
                })
                .or("疯狂星期四"::contains)
                .run((msg) -> {
                    if (random.nextDouble() < 0.8d) {
                        zibenbot.replyMsg(msg, "朋友，我没有50\n" +
                                "别再转发疯狂星期四了\n" +
                                "我建议你去吃华莱士\n" +
                                "50可以买TM七八个汉堡\n" +
                                "吃到不省人事");
                    }
                })
                .build();
    }

    @Override
    public void setUp() {

    }

    @Override
    public void run(SimpleMsg simpleMsg) {
        commander.execute(simpleMsg);
    }
}
