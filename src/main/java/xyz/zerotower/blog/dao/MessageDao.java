package xyz.zerotower.blog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import xyz.zerotower.blog.entity.Message;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
@Repository
public interface MessageDao extends BaseMapper<Message> {

}
