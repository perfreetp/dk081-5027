package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TodoCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer count;

    private List<TodoItemVO> items;
}
