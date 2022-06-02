package com.aye10032.functions;

import com.aye10032.Zibenbot;
import com.aye10032.data.historytoday.service.HistoryTodayService;
import com.aye10032.functions.funcutil.BaseFunc;
import com.aye10032.functions.funcutil.FuncExceptionHandler;
import com.aye10032.functions.funcutil.SimpleMsg;
import com.dazo66.command.Commander;
import com.dazo66.command.CommanderBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;

/**
 * @program: communismbot
 * @className: HistoryTodayFunc
 * @Description: 历史上的今天功能
 * @version: v1.0
 * @author: Aye10032
 * @date: 2022/6/2 下午 6:24
 */
public class HistoryTodayFunc extends BaseFunc {

    @Autowired
    private HistoryTodayService historyTodayService;

    private Commander<SimpleMsg> commander;

    public HistoryTodayFunc(Zibenbot zibenbot) {
        super(zibenbot);
        commander = new CommanderBuilder<SimpleMsg>()
                .seteHandler(FuncExceptionHandler.INSTENCE)
                .start()
                .or("历史上的今天"::equals)
                .run((cqmsg) -> {
                    System.out.println("历史上的今天测试");
                })
                .or("历史上的今天"::equals)
                .next()
                .run((msg) -> {
                    if (msg.getFromClient() == 2375985957L) {
                        String[] msgs = msg.getCommandPieces();
                        if (msgs.length == 2) {
                            historyTodayService.insertHistory(msgs[1], "", getDate());
                            zibenbot.replyMsg(msg, "done");
                        } else if (msgs.length == 3) {
                            historyTodayService.insertHistory(msgs[1], msgs[2], getDate());
                            zibenbot.replyMsg(msg, "done");
                        } else {
                            zibenbot.replyMsg(msg, "格式不正确！");
                        }
                    }
                })
                .pop()
                .or("历史上的明天"::equals)
                .next()
                .run((msg) -> {
                    if (msg.getFromClient() == 2375985957L) {
                        String[] msgs = msg.getCommandPieces();
                        if (msgs.length == 2) {
                            historyTodayService.insertHistory(msgs[1], "", getTomorrow());
                            zibenbot.replyMsg(msg, "done");
                        } else if (msgs.length == 3) {
                            historyTodayService.insertHistory(msgs[1], msgs[2], getTomorrow());
                            zibenbot.replyMsg(msg, "done");
                        } else {
                            zibenbot.replyMsg(msg, "格式不正确！");
                        }
                    }
                })
                .pop()
                .build();
    }

    @Override
    public void setUp() {

    }

    @Override
    public void run(SimpleMsg simpleMsg) {
        commander.execute(simpleMsg);
    }

    private String getDate() {
        Calendar calendar = Calendar.getInstance();
        String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
        String date = String.format("%02d", calendar.get(Calendar.DATE));

        return month + date;
    }

    private String getTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
        String date = String.format("%02d", calendar.get(Calendar.DATE));

        return month + date;
    }
}
