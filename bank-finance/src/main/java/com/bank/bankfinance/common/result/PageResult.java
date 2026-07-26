package com.bank.bankfinance.common.result;
import lombok.Data;
import com.bank.bankfinance.common.constant.ResponseCode;

import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private Integer size;
    private Integer current;
    private List<T> records;

    public static <T> PageResult<T> page(Long total,Integer size,Integer current,List<T> records){
        PageResult<T> pageresult = new PageResult<T>();
        pageresult.setTotal(total);
        pageresult.setSize(size);
        pageresult.setCurrent(current);
        pageresult.setRecords(records);
        return pageresult;
    }

}
