package com.topfox.service;

import com.topfox.common.DataDTO;

import java.util.List;

public interface ISuperService {

    void init();

    void beforeInit(List<DataDTO> listUpdate);
}