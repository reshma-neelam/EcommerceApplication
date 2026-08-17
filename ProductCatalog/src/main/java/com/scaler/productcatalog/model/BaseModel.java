package com.scaler.productcatalog.model;

import com.scaler.productcatalog.enums.ProductState;

import java.util.Date;

public abstract class BaseModel {
    private Long id;
    private String catalogName;
    private Date createdAt;
    private Date updatedAt;
    private ProductState state;
}
