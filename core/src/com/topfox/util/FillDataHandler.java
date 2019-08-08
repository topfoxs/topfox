package com.topfox.util;

import com.topfox.common.DataDTO;

import java.util.List;
import java.util.Map;

public interface FillDataHandler {
    void fillData(Map<String,com.topfox.data.Field> fillFields, List<DataDTO> list);
}
