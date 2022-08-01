package com.example.reactive.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author romic
 * @date 2022/1/6
 */
@Data
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    /**
     * 数据包
     */
    private T data;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 消息
     */
    private String msg;


    private Result(T data, Integer code, String msg) {
        this.data = data;
        this.code = code;
        this.msg = msg;
    }

    private Result(Integer code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    private Result(IResultCode resultCode, String msg) {
        this(resultCode, null, msg);
    }

    private Result(IResultCode resultCode, T data, String msg) {
        this(resultCode.getCode(), data, msg);
    }

    private Result(IResultCode resultCode) {
        this((T) "", resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS.getCode(), data, ResultCode.SUCCESS.getMessage());
    }


    public static <T> Result<T> fail(T data, Integer code, String message) {
        return new Result<T>(data, code, message);
    }


    public static <T> Result<T> fail(IResultCode resultCode) {
        return new Result<T>(ResultCode.FAILURE);
    }

    /**
     * 返回Result
     *
     * @param msg 消息
     * @param <T> T 泛型标记
     * @return R
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<T>(ResultCode.FAILURE, msg);
    }


}
