package com.topfox.sql;

import com.topfox.common.Component;
import com.topfox.data.TableInfo;

/**
 *
 */
public abstract class IEntitySql extends Component {
    private Condition condition;

    /**
     * 返回条件对象
     *
     * @return
     */
    public Condition where() {
        if (condition == null){
            condition = Condition.create();
            condition.setEntitySql(this);
            if (super.getTableInfo()!= null){
                condition.setTableInfo(super.getTableInfo());
            }
        }
        return condition;
    }

    public Condition setWhere(Condition where){
        condition=where;
        condition.setEntitySql(this);
        return condition;
    }

    /**
     * 构建SQL
     * @return
     */
    public abstract String getSql();

    /**
     * 清理
     */
    protected abstract void clean();
    
}