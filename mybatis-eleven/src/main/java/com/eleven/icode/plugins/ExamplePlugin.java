package com.eleven.icode.plugins;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

@Intercepts({@Signature(type = Executor.class, method = "query", args = {
        MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
})})
//@Intercepts({@Signature( type= StatementHandler.class,  method = "update", args ={Statement.class})})
public class ExamplePlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        System.out.println("代理");
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        return invocation.proceed();
    }

    // new4大对象的时候调用，所以4大对象都会被代理到Plugin
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    // 加载的时候调用， 设置属性初始化
    @Override
    public void setProperties(Properties properties) {
        System.out.println(111);
    }
}