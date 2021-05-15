package xyz.zerotower.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.zerotower.blog.dto.OperationLogDTO;
import xyz.zerotower.blog.dto.PageDTO;
import xyz.zerotower.blog.entity.OperationLog;
import xyz.zerotower.blog.vo.ConditionVO;

/**
 * @author: zerotower
 * @date: 2021-04-01
 **/
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 查询日志列表
     *
     * @param conditionVO 条件
     * @return 日志列表
     */
    PageDTO<OperationLogDTO> listOperationLogs(ConditionVO conditionVO);

}
