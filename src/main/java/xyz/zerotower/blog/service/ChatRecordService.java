package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.entity.ChatRecord;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface ChatRecordService extends IService<ChatRecord> {

    /**
     * 删除7天前的聊天记录
     */
    void deleteChartRecord();

}
