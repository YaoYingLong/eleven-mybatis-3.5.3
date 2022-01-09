/**
 * Copyright ${license.git.copyrightYears} the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.binding;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
    | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC;
  private static Constructor<Lookup> lookupConstructor;
  private final SqlSession sqlSession;
  private final Class<T> mapperInterface;
  /**
   * 用于缓存MapperMethod方法
   */
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  static {
    try {
      lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
    } catch (NoSuchMethodException e) {
      try {
        // Since Java 14+8
        lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
      } catch (NoSuchMethodException e2) {
        throw new IllegalStateException("No known constructor found in java.lang.invoke.MethodHandles.Lookup.", e2);
      }
    }
    lookupConstructor.setAccessible(true);
  }

  /**
   * 方法实现说明:Mapper接口调用目标对象
   *
   * @param proxy  -代理对象
   * @param method -目标方法
   * @param args   -目标对象参数
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 判断方法是不是Object类定义的方法，若是直接通过反射调用
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      } else if (method.isDefault()) {   // 是否接口的默认方法
        return invokeDefaultMethod(proxy, method, args); // 调用接口中的默认方法
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    /**
     * 真正的进行调用，做了两件事，把方法对象封装成一个MapperMethod对象，带有缓存作用的
     */
    final MapperMethod mapperMethod = cachedMapperMethod(method); // 把方法对象封装成一个MapperMethod对象，带有缓存作用的
    /**
     * 通过SqlSession来调用目标方法，需要研究下sqlSessionTemplate是怎么初始化的
     * 知道spring跟mybatis整合时，进行了偷天换日把mapper接口包下所有接口类型都变为了MapperFactoryBean
     * 然后发现实现了SqlSessionDaoSupport，在整合时把EmployeeMapper(案例class类型属性为MapperFactoryBean)
     * 的注入模型给改了，改成了by_type，所以会调用SqlSessionDaoSupport的setXXX方法进行赋值，从而创建了sqlSessionTemplate
     * 而在实例化sqlSessionTemplate对象时，为创建了sqlSessionTemplate的代理对象
     * this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(), new Class[] { SqlSession.class }, new SqlSessionInterceptor());
     */
    return mapperMethod.execute(sqlSession, args);
  }

  /**
   * 缓存mapper中的方法
   *
   * @param method:Mapper接口中的方法
   */
  private MapperMethod cachedMapperMethod(Method method) {
    /**
     * 相当于这句代码.jdk8的新写法
     * if(methodCache.get(method)==null){
     *     methodCache.put(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()))
     * }
     */
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }

  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
    final Class<?> declaringClass = method.getDeclaringClass();
    final Lookup lookup;
    if (lookupConstructor.getParameterCount() == 2) {
      lookup = lookupConstructor.newInstance(declaringClass, ALLOWED_MODES);
    } else {
      // SInce JDK 14+8
      lookup = lookupConstructor.newInstance(declaringClass, null, ALLOWED_MODES);
    }
    return lookup.unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }
}
