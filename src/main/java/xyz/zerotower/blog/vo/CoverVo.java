package xyz.zerotower.blog.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * @Author ZeroTower
 * @Date 2021/4/9 22:11
 * @Description
 * @Package xyz.zerotower.blog.vo
 * @PROJECT blog-service
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "封面")
public class CoverVo {

    /**
     * id
     */
    @ApiModelProperty(name = "id", value = "封面id", dataType = "Integer")
    private Integer id;

    /**
     * 菜单路径
     */
    @NotBlank(message = "菜单路径不能为空")
    @ApiModelProperty(name = "url", value = "菜单路径", required = true, dataType = "String")
    private String url;

    /**
     * 封面图片路径
     */
    @NotBlank(message = "封面图片路径不能为空")
    @ApiModelProperty(name = "coverUrl", value = "封面图片的路径", required = true, dataType = "String")
    private String coverUrl;

}
