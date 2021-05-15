package xyz.zerotower.blog.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import jdk.net.SocketFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.zerotower.blog.annotation.OptLog;
import xyz.zerotower.blog.constant.StatusConst;
import xyz.zerotower.blog.entity.Cover;
import xyz.zerotower.blog.service.CoverService;
import xyz.zerotower.blog.vo.CoverVo;
import xyz.zerotower.blog.vo.Result;

import javax.validation.Valid;
import java.sql.ResultSet;
import java.util.List;

import static xyz.zerotower.blog.constant.OptTypeConst.REMOVE;
import static xyz.zerotower.blog.constant.OptTypeConst.SAVE_OR_UPDATE;

/**
 * @Author ZeroTower
 * @Date 2021/4/9 21:59
 * @Description
 * @Package xyz.zerotower.blog.controller
 * @PROJECT blog-service
 **/
@RestController
@Api("封面操作")
public class CoverController {

    @Autowired
    protected CoverService service;

    @ApiOperation("查看封面的信息")
    @GetMapping("/admin/covers")
    Result<List<Cover>> listAllCover(){
        return new Result<>(true, StatusConst.OK,"查询成功",service.listCover());
    }


    @OptLog(optType = SAVE_OR_UPDATE)
    @ApiOperation("添加或者修改封面")
    @PostMapping("/admin/covers")
    public Result saveOrUpdateCover(@Valid @RequestBody CoverVo coverVo){
        service.saveOrUpdateCover(coverVo);
        return new Result(true,StatusConst.OK,"保存成功");
    }


    @OptLog(optType = REMOVE)
    @ApiOperation("删除封面")
    @DeleteMapping("/admin/covers")
    public Result deleteCover(@RequestBody List<Integer> idList){
        service.deleteCover(idList);
        return new Result(true,StatusConst.OK,"删除成功");
    }

}
