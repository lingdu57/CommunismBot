package com.aye10032.utils;

import net.mamoe.mirai.contact.Contact;

/**
 * @author Dazo66
 */
@FunctionalInterface
public interface IMsgUpload {

    /**
     * 上传的方法
     *
     * @param contact 上传的对象
     * @param source  上传的资源
     * @return
     */
    String upload(Contact contact, String source) throws Exception;

}
