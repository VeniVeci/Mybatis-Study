

# 什么是 MyBatis？

 MyBatis 是一款优秀的持久层框架，它支持自定义 SQL、存储过程以及高级映射。MyBatis **免除了几乎所有的 JDBC 代码以及设置参数和获取结果集的工作**。MyBatis 可以**通过简单的 XML 或注解来配置和映射原始类型**、接口和 Java POJO（Plain Old Java Objects，普通老式 Java 对象）为数据库中的记录。  
## 从 XML 中构建 SqlSessionFactory
每个基于 MyBatis 的应用都是以一个 **SqlSessionFactory** 的实例为核心的。SqlSessionFactory 的实例可以通过 SqlSessionFactoryBuilder 获得。而 SqlSessionFactoryBuilder 则可以从 XML 配置文件或一个预先配置的 Configuration 实例来构建出 SqlSessionFactory 实例。 
从 XML 文件中构建 SqlSessionFactory 的实例非常简单，建议使用类路径下的资源文件进行配置。 但也可以使用任意的输入流（InputStream）实例，比如用文件路径字符串或 file:// URL 构造的输入流。MyBatis 包含一个名叫 Resources 的工具类，它包含一些实用方法，使得从类路径或其它位置加载资源文件更加容易。 

```
String resource = "org/mybatis/example/mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
```

## 不从xml中构建
如果你更愿意直接从 Java 代码而不是 XML 文件中创建配置，或者想要创建你自己的配置构建器，MyBatis 也提供了完整的配置类，提供了所有与 XML 文件等价的配置项。 
```
DataSource dataSource = BlogDataSourceFactory.getBlogDataSource();
 TransactionFactory transactionFactory = new JdbcTransactionFactory();
 Environment environment = new Environment("development", transactionFactory, dataSource);
 Configuration configuration = new Configuration(environment);
 configuration.addMapper(BlogMapper.class);
 SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
```
注意该例中，configuration 添加了一个映射器类（mapper class）。映射器类是 Java 类，它们包含 SQL 映射注解从而避免依赖 XML 映射文件。**不过，由于 Java 注解的一些限制以及某些 MyBatis 映射的复杂性，要使用大多数高级映射（比如：嵌套联合映射）**，仍然需要使用 XML 映射文件进行映射。有鉴于此，如果存在一个同名 XML 映射文件，MyBatis 会自动查找并加载它（在这个例子中，基于类路径和 BlogMapper.class 的类名，会加载 BlogMapper.xml）。具体细节稍后讨论。 

**注解比较简单 不涉及配置文件   但是 有一些比较复杂的SQL还是需要 写xml的**
**使用注解来映射简单语句会使代码显得更加简洁，但对于稍微复杂一点的语句，Java 注解不仅力不从心**，还会让本就复杂的 SQL 语句更加混乱不堪。 因此，如果你需要做一些很复杂的操作，最好用 XML 来映射语句。 
选择何种方式来配置映射，以及是否应该要统一映射语句定义的形式，完全取决于你和你的团队。 换句话说，永远不要拘泥于一种方式，你可以很轻松地在基于注解和 XML 的语句映射方式间自由移植和切换。 

![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679713661113-9e494441-ba9f-4b47-b78a-d4c2da74f54a.png#averageHue=%23f4f3f2&clientId=uc8b02c1d-0d97-4&from=paste&height=438&id=uea2f2379&name=image.png&originHeight=547&originWidth=1451&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=97052&status=done&style=none&taskId=ud0766909-a0b1-465a-ac6a-73587f3f00b&title=&width=1160.8)

记**得关闭session**
## 探究已映射的 SQL 语句
### 对命名空间的一点补充
在之前版本的 MyBatis 中，**命名空间（Namespaces）**的作用并不大，是可选的。 但现在，**随着命名空间越发重要，你必须指定命名空间。 **
命名空间的作用有两个，一个是利用更长的全限定名来将不同的语句隔离开来，同时也实现了你上面见到的接口绑定。就算你觉得暂时用不到接口绑定，你也应该遵循这里的规定，以防哪天你改变了主意。 长远来看，只要将命名空间置于合适的 Java 包命名空间之中，你的代码会变得更加整洁，也有利于你更方便地使用 MyBatis。 
**命名解析：**为了减少输入量，MyBatis 对所有具有名称的配置元素（包括语句，结果映射，缓存等）使用了如下的命名解析规则。 

- 全限定名（比如 “com.mypackage.MyMapper.selectAllThings）将被直接用于查找及使用。 
- 短名称（比如 “selectAllThings”）如果全局唯一也可以作为一个单独的引用。 如果不唯一，有两个或两个以上的相同名称（比如 “com.foo.selectAllThings” 和 “com.bar.selectAllThings”），那么使用时就会产生“短名称不唯一”的错误，这种情况下就必须使用全限定名。 


## 注解和xml
```
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.mybatis.example.BlogMapper">
  <select id="selectBlog" resultType="Blog">
    select * from Blog where id = #{id}
  </select>
</mapper>
```
```
package org.mybatis.example;
public interface BlogMapper {
  @Select("SELECT * FROM blog WHERE id = #{id}")
  Blog selectBlog(int id);
}
```


## 作用域（Scope）和生命周期

 理解我们之前讨论过的不同作用域和生命周期类别是至关重要的，**因为错误的使用会导致非常严重的并发问题。  **

### SqlSessionFactoryBuilder
这个类可以被实例化、使用和丢弃，一旦创建了 SqlSessionFactory，就不再需要它了。 因此 SqlSessionFactoryBuilder 实例的最佳作用域是方法作用域（也就是局部方法变量）。 **你可以重用 SqlSessionFactoryBuilder 来创建多个 SqlSessionFactory 实例**，但最好还是不要一直保留着它，以保证所有的 XML 解析资源可以被释放给更重要的事情。 
示例代码：
这个里面只有一个sqlSessionFactory  用来生成不同的session
```java

public class MybatisUtils {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取SqlSession连接  是一个 对象  包含 环境  执行器  事务相关
    public static SqlSession getSession(){
        
        return sqlSessionFactory.openSession();
    }

}

```
### SqlSessionFactory
SqlSessionFactory 一旦被创建就应该在应用的运行期间一直存在，没有任何理由丢弃它或重新创建另一个实例。 使用 SqlSessionFactory 的最佳实践是在应用运行期间不要重复创建多次，多次重建 SqlSessionFactory 被视为一种代码“坏习惯”。因此 SqlSessionFactory 的最佳作用域是应用作用域。 有很多方法可以做到，最简单的就是使用单例模式或者静态单例模式。 参见上文
### SqlSession
**每个线程都应该有它自己的 SqlSession 实例。**SqlSession 的实例不是线程安全的，因此是不能被共享的，所以它的最佳的作用域是**请求或方法作用域**。 绝对不能将 SqlSession 实例的引用放在一个类的静态域，甚至一个类的实例变量也不行。 也绝不能将 SqlSession 实例的引用放在任何类型的托管作用域中，比如 Servlet 框架中的 HttpSession。 如果你现在正在使用一种 Web 框架，考虑将 SqlSession 放在一个和 HTTP 请求相似的作用域中。 换句话说，每次收到 HTTP 请求，就可以打开一个 SqlSession，返回一个响应后，就关闭它。 这个关闭操作很重要，为了确保每次都能执行关闭操作，你应该把这个关闭操作放到 finally 块中。 下面的示例就是一个确保 SqlSession 关闭的标准模式： 
```java
try (SqlSession session = sqlSessionFactory.openSession()) {
   // 你的应用逻辑代码
 }
```
在所有代码中都遵循这种使用模式，可以保证所有数据库资源都能被正确地关闭。
### 映射器实例
映射器是一些绑定映射语句的接口。映射器接口的实例是从 SqlSession 中获得的。虽然从技术层面上来讲，任何映射器实例的最大作用域与请求它们的 SqlSession 相同。但方法作用域才是映射器实例的最合适的作用域。 也就是说，映射器实例应该在调用它们的方法中被获取，使用完毕之后即可丢弃。 映射器实例并不需要被显式地关闭。尽管在整个请求作用域保留映射器实例不会有什么问题，但是你很快会发现，在这个作用域上管理太多像 SqlSession 的资源会让你忙不过来。 因此，**最好将映射器放在方法作用域内**。就像下面的例子一样： 
```java
try (SqlSession session = sqlSessionFactory.openSession()) {
   BlogMapper mapper = session.getMapper(BlogMapper.class);
   // 你的应用逻辑代码
 }
```



# XML配置相关
 MyBatis 的配置文件包含了会**深深影响 MyBatis 行为的设置和属性信息**。 配置文档的顶层结构如下：  


## 环境配置（environments）
MyBatis 可以配置成适应多种环境，这种机制有助于将 SQL 映射应用于多种数据库之中， 现实情况下有多种理由需要这么做。例如，开发、测试和生产环境需要有不同的配置；或者想在具有相同 Schema 的多个生产数据库中使用相同的 SQL 映射。还有许多类似的使用场景。
**不过要记住：尽管可以配置多个环境，但每个 SqlSessionFactory 实例只能选择一种环境。**
所以，如果你想连接两个数据库，就需要创建两个 SqlSessionFactory 实例，每个数据库对应一个。而如果是三个数据库，就需要三个实例，依此类推，记起来很简单： 

- **每个数据库对应一个 SqlSessionFactory 实例**

为了指定创建哪种环境，只要将它作为可选的参数传递给 SqlSessionFactoryBuilder 即可。可以接受环境配置的两个方法签名是： 
```java
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, environment);
 SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, environment, properties);
```
如果忽略了环境参数，那么将会加载默认环境，如下所示： 
```java
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
 SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, properties);
```
注意一些关键点: 

-  默认使用的环境 ID（比如：default="development"）。 
-  每个 environment 元素定义的环境 ID（比如：id="development"）。 
-  事务管理器的配置（比如：type="JDBC"）。 
-  数据源的配置（比如：type="POOLED"）。 


**事务管理器（transactionManager）**
在 MyBatis 中有两种类型的事务管理器（也就是** type="[JDBC|MANAGED]"）**：

-  JDBC – 这个配置直接使用了 JDBC 的提交和回滚功能，它依赖从数据源获得的连接来管理事务作用域。默认情况下，为了与某些驱动程序兼容，它在关闭连接时启用自动提交。然而，对于某些驱动程序来说，启用自动提交不仅是不必要的，而且是一个代价高昂的操作。因此，从 3.5.10 版本开始，你可以通过将 "skipSetAutoCommitOnClose" 属性设置为 "true" 来跳过这个步骤。例如： 
```java
<transactionManager type="JDBC">
   <property name="skipSetAutoCommitOnClose" value="true"/>
 </transactionManager>
```

-  MANAGED – 这个配置几乎没做什么。它从不提交或回滚一个连接，而是让容器来管理事务的整个生命周期（比如 JEE 应用服务器的上下文）。 默认情况下它会关闭连接。然而一些容器并不希望连接被关闭，因此需要将 closeConnection 属性设置为 false 来阻止默认的关闭行为。例如: <transactionManager type="MANAGED">   <property name="closeConnection" value="false"/> </transactionManager>

提示 如果你正在使用 Spring + MyBatis，**则没有必要配置事务管理器，因为 Spring 模块会使用自带的管理器来覆盖前面的配置。 **
### 不太懂
这两种事务管理器类型都不需要设置任何属性。它们其实是类型别名，**换句话说，你可以用 TransactionFactory 接口实现类的全限定名或类型别名代替它们。 **

```java
public interface TransactionFactory {
  default void setProperties(Properties props) { // 从 3.5.2 开始，该方法为默认方法
    // 空实现
  }
  Transaction newTransaction(Connection conn);
  Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);
}
```
 在事务管理器实例化后，所有在 XML 中配置的属性将会被传递给 setProperties() 方法。你的实现还需要创建一个 Transaction 接口的实现类，这个接口也很简单：  
```java
public interface Transaction {
  Connection getConnection() throws SQLException;
  void commit() throws SQLException;
  void rollback() throws SQLException;
  void close() throws SQLException;
  Integer getTimeout() throws SQLException;
}
```
 使用这两个接口，你可以完全自定义 MyBatis 对事务的处理。  



## 数据源（dataSource）
dataSource 元素使用标准的 JDBC 数据源接口来配置 JDBC 连接对象的资源。

- 大多数 MyBatis 应用程序会按示例中的例子来配置数据源。虽然数据源配置是可选的，**但如果要启用延迟加载特性，就必须配置数据源。 **

有三种内建的数据源类型（也就是 type="[UNPOOLED|POOLED|JNDI]"）：

**UNPOOLED**– 这个数据源的实现**会每次请求时打开和关闭连接**。虽然有点慢，但对那些数据库连接可用性要求不高的简单应用程序来说，是一个很好的选择。 性能表现则依赖于使用的数据库，对某些数据库来说，使用连接池并不重要，这个配置就很适合这种情形。UNPOOLED 类型的数据源仅仅需要配置以下 5 种属性：

- driver – 这是 JDBC 驱动的 Java 类全限定名（并不是 JDBC 驱动中可能包含的数据源类）。 
- url – 这是数据库的 JDBC URL 地址。 
- username – 登录数据库的用户名。 
- password – 登录数据库的密码。 
- defaultTransactionIsolationLevel – 默认的连接事务隔离级别。 
- defaultNetworkTimeout – 等待数据库操作完成的默认网络超时时间（单位：毫秒）。查看 java.sql.Connection#setNetworkTimeout() 的 API 文档以获取更多信息。 

作为可选项，你也可以传递属性给数据库驱动。只需在属性名加上“driver.”前缀即可，例如： 

- driver.encoding=UTF8

这将通过 DriverManager.getConnection(url, driverProperties) 方法传递值为 UTF8 的 encoding 属性给数据库驱动。 


**POOLED**– **这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来，避免了创建新的连接实例时所必需的初始化和认证时间**。 这种处理方式很流行，能使并发 Web 应用快速响应请求。 
除了上述提到 UNPOOLED 下的属性外，还有更多属性用来配置 POOLED 的数据源：

- **poolMaximumActiveConnections **– 在任意时间可存在的活动（正在使用）连接数量，默认值：10 
- poolMaximum**Idle**Connections – 任意时间可能存在的空闲连接数。 
- poolMaximumCheckoutTime – 在被强制返回之前，池中连接被检出（checked out）时间，默认值：**20000 毫秒（即 20 秒） **
- poolTimeToWait – 这是一个底层设置，如果获取连接花费了相当长的时间，连接池会打印状态日志并重新尝试获取一个连接**（避免在误配置的情况下一直失败且不打印日志），默认值：20000 毫秒（即 20 秒）**。 
- poolMaximumLocalBadConnectionTolerance – 这是一个关于坏连接容忍度的底层设置， 作用于每一个尝试从缓存池获取连接的线程。 如果这个线程获取到的是一个坏的连接，那么这个数据源允许这个线程尝试重新获取一个新的连接，但是这个重新尝试的次数不应该超过 poolMaximumIdleConnections 与 poolMaximumLocalBadConnectionTolerance 之和。 默认值：3（新增于 3.4.5） 
- poolPingQuery – 发送到数据库的侦测查询，用来检验连接是否正常工作并准备接受请求。默认是“NO PING QUERY SET”，这会导致多数数据库驱动出错时返回恰当的错误消息。 
- poolPingEnabled – 是否启用侦测查询。若开启，需要设置 poolPingQuery 属性为一个可执行的 SQL 语句（最好是一个速度非常快的 SQL 语句），默认值：false。 
- poolPingConnectionsNotUsedFor – 配置 poolPingQuery 的频率。可以被设置为和数据库连接超时时间一样，来避免不必要的侦测，默认值：0（即所有连接每一时刻都被侦测 — 当然仅当 poolPingEnabled 为 true 时适用）。 

## 映射器（mappers）
 你可以使用相对于类路径的资源引用，或完全限定资源定位符（包括 file:/// 形式的 URL），**或类名和包名等**。例如：  
**包名 > 绝对路径 > 相对路径  > 类路径 **
**在xml中只能有一类**
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679747237061-034ed05b-b0b7-4b92-925e-a437f17d0ac1.png#averageHue=%23f5f4f3&clientId=ua5a0a1a3-f781-4&from=paste&height=607&id=u1cd5372b&name=image.png&originHeight=759&originWidth=710&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=80395&status=done&style=none&taskId=u96872231-ee0a-495d-80d9-b67875058ae&title=&width=568)


## 属性（properties）
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679799626689-66bfd6ef-845a-4a9c-be5e-e7e849e8621c.png#averageHue=%23f5f4f4&clientId=u151fe09b-9b75-4&from=paste&height=361&id=u3d41eef8&name=image.png&originHeight=451&originWidth=1272&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=54537&status=done&style=none&taskId=u90dc2762-6cbd-4dc1-b581-dab1aa77079&title=&width=1017.6)

如果一个属性在不只一个地方进行了配置，那么，MyBatis 将按照下面的顺序来加载：

- 首先读取在 properties 元素体内指定的属性。 
- 然后根据 properties 元素中的 resource 属性读取类路径下属性文件，或根据 url 属性指定的路径读取属性文件，并覆盖之前读取过的同名属性。 
- 最后读取作为方法参数传递的属性，并覆盖之前读取过的同名属性。 

因此，**通过方法参数传递的属性具有最高优先级，resource/url 属性中指定的配置文件次之，最低优先级的则是 properties 元素中指定的属性。**
## 设置（settings）
 **这是 MyBatis 中极为重要的调整设置，它们会改变 MyBatis 的运行时行为。 ** 
太多了 只需要记住几个重要的  常用的即可

可以做一些实验

## 类型别名（typeAliases）
 类型别名可为 Java 类型设置一个缩写名字。 它仅用于 XML 配置，**意在降低冗余的全限定类名书写。  **
```java
<typeAliases>
  <typeAlias alias="Author" type="domain.blog.Author"/>
  <typeAlias alias="Blog" type="domain.blog.Blog"/>
  <typeAlias alias="Comment" type="domain.blog.Comment"/>
  <typeAlias alias="Post" type="domain.blog.Post"/>
  <typeAlias alias="Section" type="domain.blog.Section"/>
  <typeAlias alias="Tag" type="domain.blog.Tag"/>
</typeAliases>
```
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679800200362-78962f71-5684-4988-94fd-15db7fcaa46c.png#averageHue=%23f5f4f3&clientId=u151fe09b-9b75-4&from=paste&height=499&id=u5031bdf4&name=image.png&originHeight=624&originWidth=852&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=76299&status=done&style=none&taskId=u056f7f4d-3c69-4724-9db7-6aa0091e4d8&title=&width=681.6)


## 类型处理器（typeHandlers）
 **MyBatis** 在设置预处理语句（PreparedStatement）中的参数或从结果集中取出一个值时，** 都会用类型处理器将获取到的值以合适的方式转换成 Java 类型**。下表描述了一些默认的类型处理器  。
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679800454346-3a81146a-3a62-478a-b7d2-809336693e7a.png#averageHue=%23e9bc89&clientId=u151fe09b-9b75-4&from=paste&height=365&id=u1df7ad23&name=image.png&originHeight=456&originWidth=1077&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=53134&status=done&style=none&taskId=u808e7556-8748-4712-b254-3fe288bb75f&title=&width=861.6)![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1679800657503-06fa61f2-b70b-4659-a815-4ecd1075f872.png#averageHue=%23f0b388&clientId=u151fe09b-9b75-4&from=paste&height=305&id=u34711101&name=image.png&originHeight=381&originWidth=955&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=25519&status=done&style=none&taskId=uac8e5d78-885c-4fca-8d99-f52fc3e9539&title=&width=764)


** 你可以重写已有的类型处理器或创建你自己的类型处理器来处理不支持的或非标准的类型。 具体做法为：实现 org.apache.ibatis.type.TypeHandler 接口，** 或继承一个很便利的类 org.apache.ibatis.type.BaseTypeHandler， 并且可以（可选地）将它映射到一个 JDBC 类型。比如：  

```java
// ExampleTypeHandler.java
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ExampleTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, parameter);
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getString(columnName);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getString(columnIndex);
  }
}
```
```java
<!-- mybatis-config.xml -->
<typeHandlers>
  <typeHandler handler="org.mybatis.example.ExampleTypeHandler"/>
</typeHandlers>
```
 使用上述的类型处理器将会覆盖已有的处理 Java String 类型的属性以及 VARCHAR 类型的参数和结果的类型处理器。 要注意 MyBatis 不会通过检测数据库元信息来决定使用哪种类型，**所以你必须在参数和结果映射中指明字段是 VARCHAR 类型**， 以使其能够绑定到正确的类型处理器上。这是因为 MyBatis 直到语句被执行时才清楚数据类型。  


通过类型处理器的泛型，MyBatis 可以得知该类型处理器处理的 Java 类型，不过这种行为可以通过两种方法改变： 

-  在类型处理器的配置元素（typeHandler 元素）上增加一个 javaType 属性（比如：javaType="String"）； 
-  在类型处理器的类上增加一个 @MappedTypes 注解指定与其关联的 Java 类型列表。 如果在 javaType 属性中也同时指定，则注解上的配置将被忽略。 

可以通过两种方式来指定关联的 JDBC 类型：

-  在类型处理器的配置元素上增加一个 jdbcType 属性（比如：jdbcType="VARCHAR"）； 
-  在类型处理器的类上增加一个 @MappedJdbcTypes 注解指定与其关联的 JDBC 类型列表。 如果在 jdbcType 属性中也同时指定，则注解上的配置将被忽略。 


## 对象工厂（objectFactory）
 每次 MyBatis 创建**结果对象的新实例时**，它都会使用一个对象工厂（ObjectFactory）实例来完成实例化工作。 默认的对象工厂需要做的仅仅是实例化目标类，要么通过默认无参构造方法，**要么通过存在的参数映射来调用带有参数的构造方法**。 如果想覆盖对象工厂的默认行为，可以通过创建自己的对象工厂来实现。比如  




## 插件（plugins）

MyBatis 允许你在映射语句执行过程中的某一点进行拦截调用。默认情况下，MyBatis 允许使用插件来拦截的方法调用包括： 

-  **Executor** (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed) 
-  **ParameterHandler** (getParameterObject, setParameters) 
-  **ResultSetHandler** (handleResultSets, handleOutputParameters) 
-  **StatementHandler** (prepare, parameterize, batch, update, query) 

这些类中方法的细节可以通过查看每个方法的签名来发现，或者直接查看 MyBatis 发行包中的源代码。 如果你想做的不仅仅是监控方法的调用，那么你最好相当了解要重写的方法的行为。 因为在试图修改或重写已有方法的行为时，很可能会破坏 MyBatis 的核心模块。 这些都是更底层的类和方法，所以使用插件的时候要特别当心。
通过 MyBatis 提供的强大机制，使用插件是非常简单的，只需实现 Interceptor 接口，并指定想要拦截的方法签名即可。



# XML 映射器
 MyBatis 的真正强大在于它的语句映射，这是它的魔力所在。由于它的异常强大，映射器的 XML 文件就显得相对简单。**如果拿它跟具有相同功能的 JDBC 代码进行对比，你会立即发现省掉了将近 95% 的代码。**MyBatis 致力于减少使用成本，让用户能更专注于 SQL 代码。  
SQL 映射文件只有很少的几个顶级元素（按照应被定义的顺序列出）：

- cache – 该命名空间的缓存配置。 
- cache-ref – 引用其它命名空间的缓存配置。 
- resultMap – 描述如何从数据库结果集中加载对象，是最复杂也是最强大的元素。 
- ~~parameterMap – 老式风格的参数映射。此元素已被废弃，并可能在将来被移除！请使用行内参数映射。文档中不会介绍此元素。 ~~
- sql – 可被其它语句引用的可重用语句块。 
- insert – 映射插入语句。 
- update – 映射更新语句。 
- delete – 映射删除语句。 
- select – 映射查询语句。 


## select
查询语句是 MyBatis 中最常用的元素之一——光能把数据存到数据库中价值并不大，还要能重新取出来才有用，多数应用也都是查询比修改要频繁。 MyBatis 的基本原则之一是：在每个插入、更新或删除操作之间，通常会执行多个查询操作。因此，MyBatis 在查询和结果映射做了相当多的改进。一个简单查询的 select 元素是非常简单的。比如： 

```java
<select id="selectPerson" parameterType="int" resultType="hashmap">
  SELECT * FROM PERSON WHERE ID = #{id}
</select>
```
这个语句名为 selectPerson，接受一个 int（或 Integer）类型的参数，并返回一个 HashMap 类型的对象，其中的键是列名，值便是结果行中的对应值。 
注意参数符号：
#{id}
这就告诉 MyBatis 创建一个预处理语句（PreparedStatement）参数，在 JDBC 中，这样的一个参数在 SQL 中会由一个“?”来标识，并被传递到一个新的预处理语句中，就像这样： 
// 近似的 JDBC 代码，非 MyBatis 代码... 
```java
String selectPerson = "SELECT * FROM PERSON WHERE ID=?";
 PreparedStatement ps = conn.prepareStatement(selectPerson);
 ps.setInt(1,id);
```

```java
<select
  id="selectPerson"
  parameterType="int"
  parameterMap="deprecated"
  resultType="hashmap"
  resultMap="personResultMap"
  flushCache="false"
  useCache="true"
  timeout="10"
  fetchSize="256"
  statementType="PREPARED"
  resultSetType="FORWARD_ONLY">
```
| parameterType |  将会传入这条语句的参数的类全限定名或别名。这个属性是可选的，**因为 MyBatis 可以根据语句中实际传入的参数计算出应该使用的类型处理器（TypeHandler）**，默认值为未设置（unset）。  |
| --- | --- |

| resultType |  期望从这条语句中返回结果的类全限定名或别名。 注意，如果返回的是集合，那应该设置为集合包含的类型，而不是集合本身的类型。 resultType 和 resultMap 之间只能同时使用一个。  |
| --- | --- |
| resultMap |  对外部 resultMap 的命名引用。结果映射是 MyBatis 最强大的特性，如果你对其理解透彻，许多复杂的映射问题都能迎刃而解。 resultType 和 resultMap 之间只能同时使用一个。  |

| flushCache |  将其设置为 true 后，只要语句被调用，都会导致本地缓存和二级缓存被清空，默认值：false。  |
| --- | --- |
| useCache |  将其设置为 true 后，将会导致本条语句的结果被二级缓存缓存起来，默认值：对 select 元素为 true。  |

flushCache和useCache
一级缓存是SqlSession级别的缓存。在操作数据库时需要构造 sqlSession对象，在对象中有一个数据结构（HashMap）用于存储缓存数据。不同的sqlSession之间的缓存数据区域（HashMap）是互相不影响的。
二级缓存是mapper级别的缓存，多个SqlSession去操作同一个Mapper的sql语句，多个SqlSession可以共用二级缓存，二级缓存是跨SqlSession的。
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1680007279198-6498386c-989e-445e-a160-3cb597235990.png#averageHue=%23f5f5f5&clientId=u058e6d00-6f23-4&from=paste&height=331&id=u2acd630f&name=image.png&originHeight=414&originWidth=653&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=87709&status=done&style=none&taskId=u0dd62e27-8ceb-49d7-a168-f5e2e0ae9c6&title=&width=522.4)

 第一次发起查询用户id为1的用户信息，先去找缓存中是否有1的用户，如果有的话拿去用，如果没有去数据库中查去。得到用户信息放入一级缓存中去。如果SqlSession去执行commit操作（执行插入、删除、更新）的话，清空SqlSession中的一级缓存，这样做就是为了让缓存中的数据保持最新，避免用户读到错误的数据。  

![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1680007318934-5da0b276-a580-4e97-b9ee-efdc350ec733.png#averageHue=%23f5f5f5&clientId=u058e6d00-6f23-4&from=paste&height=333&id=u1be2da92&name=image.png&originHeight=416&originWidth=773&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=101300&status=done&style=none&taskId=u97686f1b-2cb5-45fc-873e-a94c717023c&title=&width=618.4)

首先得开启二级缓存，sqlSession1去查询用户id为1的用户信息，查询到用户信息会将查询数据存储到二级缓存中。
如果SqlSession3去执行相同 mapper下sql，执行commit提交，清空该 mapper下的二级缓存区域的数据。sqlSession2去查询用户
id为1的用户信息，去缓存中找是否存在数据，如果存在直接从缓存中取出数据。

MyBatis中开启二级缓存及flushCache与useCache的使用

第一步：Configuration.xml设置二级缓存的总开关，
```java
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
<settings>
<!-- 全局映射器启用缓存 -->
<setting name="cacheEnabled" value="true" />
<!-- 查询时，关闭关联对象即时加载以提高性能 -->
<setting name="lazyLoadingEnabled" value="false" />
<!-- 对于未知的SQL查询，允许返回不同的结果集以达到通用的效果 -->
<setting name="multipleResultSetsEnabled" value="true" />
<!-- 允许使用列标签代替列名 -->
<setting name="useColumnLabel" value="true" />
<!-- 对于批量更新操作缓存SQL以提高性能  -->
<setting name="defaultExecutorType" value="REUSE" />
<!-- 数据库超过25000秒仍未响应则超时 -->
<setting name="defaultStatementTimeout" value="25000" />
</settings>
<mappers>
<!--<mapper resource="dao/mysql/CtdAuthCommonMapper.xml"/>-->
</mappers>
</configuration>
```


第二步：在具体的mapper.xml中开启二级缓存。
 在MyBatis的XML文件中可以
```java
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="com.ctd.cmp.loganalyse.bean.pojo.mapper.CtdBizMetricsMapper">
<!-- 缓存10分钟 -->
<cache eviction="FIFO" flushInterval="600000" size="4096" readOnly="true"/>


<select id="save" parameterType="XX" flushCache="true" useCache="false"> </select>
</mapper>
```
各个属性意义如下：

- eviction：缓存回收策略 
   - LRU：最少使用原则，移除最长时间不使用的对象
   - FIFO：先进先出原则，按照对象进入缓存顺序进行回收
   - SOFT：软引用，移除基于垃圾回收器状态和软引用规则的对象
   - WEAK：弱引用，更积极的移除移除基于垃圾回收器状态和弱引用规则的对象
- flushInterval：刷新时间间隔，单位为毫秒，这里配置的100毫秒。如果不配置，那么只有在进行数据库修改操作才会被动刷新缓存区
- size：引用额数目，代表缓存最多可以存储的对象个数
- readOnly：是否只读，如果为true，则所有相同的sql语句返回的是同一个对象**（有助于提高性能，但并发操作同一条数据时，可能不安全）**，如果设置为false，则相同的sql，后面访问的是cache的clone副本。

可以在Mapper的具体方法下设置对二级缓存的访问意愿：
```java
<select id="save" parameterType="XX" flushCache="true" useCache="false"> </select>
```
如果没有去配置flushCache、useCache，那么默认是启用缓存的

- **flushCache**默认为false，表示任何时候语句被调用，都不会去清空本地缓存和二级缓存。
- **useCache**默认为true，表示会将本条语句的结果进行二级缓存。
- 在insert、update、delete语句时： flushCache默认为true，表示任何时候语句被调用，都会导致本地缓存和二级缓存被清空。** useCache属性在该情况下没有用**。update 的时候如果 flushCache="false"，**则当你更新后，查询的数据数据还是老的数据。**



## insert, update 和 delete
```java
<insert id="insertAuthor">
  insert into Author (id,username,password,email,bio)
  values (#{id},#{username},#{password},#{email},#{bio})
</insert>

<update id="updateAuthor">
  update Author set
    username = #{username},
    password = #{password},
    email = #{email},
    bio = #{bio}
  where id = #{id}
</update>

<delete id="deleteAuthor">
  delete from Author where id = #{id}
</delete>
```
如前所述，插入语句的配置规则更加丰富，在插入语句里面有一些额外的属性和子元素用来处理主键的生成，并且提供了多种生成方式。
首先，如果你的数据库支持自动生成主键的字段（比如 MySQL 和 SQL Server），那么你可以设置 useGeneratedKeys=”true”，然后再把 keyProperty 设置为目标属性就 OK 了。例如，如果上面的 Author 表已经在 id 列上使用了自动生成，那么语句可以修改为：
```java
<insert id="insertAuthor" useGeneratedKeys="true"
    keyProperty="id">
  insert into Author (username,password,email,bio)
  values (#{username},#{password},#{email},#{bio})
</insert>
```
```java
<insert id="insertAuthor" useGeneratedKeys="true"
    keyProperty="id">
  insert into Author (username, password, email, bio) values
  <foreach item="item" collection="list" separator=",">
    (#{item.username}, #{item.password}, #{item.email}, #{item.bio})
  </foreach>
</insert>
```


As an irregular case, some databases allow INSERT, UPDATE or DELETE statement to return result set **(e.g. RETURNING clause of PostgreSQL and MariaDB or OUTPUT clause of MS SQL Server). **This type of statement must be written as <select> to map the returned data. 
```java
<select id="insertAndGetAuthor" resultType="domain.blog.Author"
       affectData="true" flushCache="true">
   insert into Author (username, password, email, bio)
   values (#{username}, #{password}, #{email}, #{bio})
   returning id, username, password, email, bio
 </select>
```

## 参数
之前见到的所有语句都使用了简单的参数形式。但实际上，参数是 MyBatis 非常强大的元素。对于大多数简单的使用场景，你都不需要使用复杂的参数，比如： 
```java
<select id="selectUsers" resultType="User">
   select id, username, password
   from users
   where id = #{id}
 </select>
```
上面的这个示例说明了一个非常简单的命名参数映射。鉴于参数类型（parameterType）会被自动设置为 int，这个参数可以随意命名。原始类型或简单数据类型（比如 Integer 和 String）因为没有其它属性，会用它们的值来作为参数。 然而，如果传入一个复杂的对象，行为就会有点不一样了。比如： 
```java
<insert id="insertUser" parameterType="User">
   insert into users (id, username, password)
   values (#{id}, #{username}, #{password})
 </insert>
```
如果 User 类型的参数对象传递到了语句中，会查找 id、username 和 password 属性，然后将它们的值传入预处理语句的参数中。 
对传递语句参数来说，这种方式真是干脆利落。不过参数映射的功能远不止于此。 
首先，和 MyBatis 的其它部分一样，参数也可以指定一个特殊的数据类型。 
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1680052942126-de83fe04-2f3e-451f-b548-5107f515847f.png#averageHue=%23f4f2f0&clientId=u849bc7e4-4c3c-4&from=paste&height=208&id=u1e32e1d3&name=image.png&originHeight=260&originWidth=1547&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=96149&status=done&style=none&taskId=u3296f084-819c-4df2-9a35-d939e9cf730&title=&width=1237.6)

## 字符串替换 
默认情况下，使用 #{} 参数语法时，MyBatis 会创建 PreparedStatement 参数占位符，并通过占位符安全地设置参数（就像使用 ? 一样）。 这样做更安全，更迅速，通常也是首选做法，不过有时你就是想直接在 SQL 语句中直接插入一个不转义的字符串。 比如 ORDER BY 子句，这时候你可以： 
**ORDER BY ${columnName}**
这样，MyBatis 就不会修改或转义该字符串了。
当 SQL 语句中的元数据（如表名或列名）是动态生成的时候，字符串替换将会非常有用。 举个例子，如果你想 select 一个表任意一列的数据时，不需要这样写： 
```java
@Select("select * from user where id = #{id}")
 User findById(@Param("id") long id);
 
 @Select("select * from user where name = #{name}")
 User findByName(@Param("name") String name);
 
 @Select("select * from user where email = #{email}")
 User findByEmail(@Param("email") String email);
 
 // 其它的 "findByXxx" 方法
```
 而是可以只写这样一个方法： 
```java
@Select("select * from user where ${column} = #{value}")
 User findByColumn(@Param("column") String column, @Param("value") String value);
```
 其中 ${column} 会被直接替换，而 #{value} 会使用 ? 预处理。 这样，就能完成同样的任务： 
```java
User userOfId1 = userMapper.findByColumn("id", 1L);
 User userOfNameKid = userMapper.findByColumn("name", "kid");
 User userOfEmail = userMapper.findByColumn("email", "noone@nowhere.com");
```
这种方式也同样适用于替换表名的情况。 
提示 用这种方式接受用户的输入，并用作语句参数是不安全的，会导致潜在的 SQL 注入攻击。因此，要么不允许用户输入这些字段，要么自行转义并检验这些参数。 


## 结果映射
resultMap 元素是 MyBatis 中最重要最强大的元素。它可以让你从 90% 的 JDBC ResultSets 数据提取代码中解放出来，并在一些情形下允许你进行一些 JDBC 不支持的操作。实际上，在为一些比如连接的复杂语句编写映射代码的时候，一份 resultMap 能够代替实现同等功能的数千行代码。**ResultMap 的设计思想是，对简单的语句做到零配置，对于复杂一点的语句，只需要描述语句之间的关系就行了。** 
之前你已经见过简单映射语句的示例，它们没有显式指定 resultMap。比如： 
```java
<select id="selectUsers" resultType="map">
   select id, username, hashedPassword
   from some_table
   where id = #{id}
 </select>
```
上述语句只是简单地将所有的列映射到 HashMap 的键上，这由 resultType 属性指定。虽然在大部分情况下都够用，但是 HashMap 并不是一个很好的领域模型。你的程序更可能会使用 JavaBean 或 POJO（Plain Old Java Objects，普通老式 Java 对象）作为领域模型。MyBatis 对两者都提供了支持。看看下面这个 JavaBean： 
```java
package com.someapp.model;
 public class User {
   private int id;
   private String username;
   private String hashedPassword;
 
   public int getId() {
     return id;
   }
   public void setId(int id) {
     this.id = id;
   }
   public String getUsername() {
     return username;
   }
   public void setUsername(String username) {
     this.username = username;
   }
   public String getHashedPassword() {
     return hashedPassword;
   }
   public void setHashedPassword(String hashedPassword) {
     this.hashedPassword = hashedPassword;
   }
 }
```
基于 JavaBean 的规范，上面这个类有 3 个属性：id，username 和 hashedPassword。这些属性会对应到 select 语句中的列名。 
这样的一个 JavaBean 可以被映射到 ResultSet，就像映射到 HashMap 一样简单。 
```java
<select id="selectUsers" resultType="com.someapp.model.User">
   select id, username, hashedPassword
   from some_table
   where id = #{id}
 </select>
```

在这些情况下，MyBatis 会在幕后自动创建一个 ResultMap，再根据属性名来映射列到 JavaBean 的属性上。如果列名和属性名不能匹配上，可以在 SELECT 语句中设置列别名（这是一个基本的 SQL 特性）来完成匹配。比如： 
```java
<select id="selectUsers" resultType="User">
   select
     user_id             as "id",
     user_name           as "userName",
     hashed_password     as "hashedPassword"
   from some_table
   where id = #{id}
 </select>
```
在学习了上面的知识后，你会发现上面的例子没有一个需要显式配置 ResultMap，这就是 ResultMap 的优秀之处——你完全可以不用显式地配置它们。 虽然上面的例子不用显式配置 ResultMap。 但为了讲解，我们来看看如果在刚刚的示例中，显式使用外部的 resultMap 会怎样，这也是解决列名不匹配的另外一种方式。 
```java
<resultMap id="userResultMap" type="User">
   <id property="id" column="user_id" />
   <result property="username" column="user_name"/>
   <result property="password" column="hashed_password"/>
 </resultMap>
```
然后在引用它的语句中设置 resultMap 属性就行了（注意我们去掉了 resultType 属性）。比如: 
```java
<select id="selectUsers" resultMap="userResultMap">
   select user_id, user_name, hashed_password
   from some_table
   where id = #{id}
 </select>
```
如果这个世界总是这么简单就好了。 



## 高级结果映射
MyBatis 创建时的一个思想是：数据库不可能永远是你所想或所需的那个样子。 我们希望每个数据库都具备良好的第三范式或 BCNF 范式，可**惜它们并不都是那样。 如果能有一种数据库映射模式，完美适配所有的应用程序，那就太好了，但可惜也没有**。 而 ResultMap 就是 MyBatis 对这个问题的答案。 
比如，我们如何映射下面这个语句？ 

#### id & result
<id property="id" column="post_id"/> <result property="subject" column="post_subject"/>
这些元素是结果映射的基础。_id_ 和 _result_ 元素都将一个列的值映射到一个简单数据类型（String, int, double, Date 等）的属性或字段。 
这两者之间的唯一不同是，_id_ 元素对应的属性会被标记为对象的标识符，在比较对象实例时使用。 这样可以提高整体的性能，尤其是进行缓存和嵌套结果映射（也就是连接映射）的时候。 
两个元素都有一些属性： 
![image.png](https://cdn.nlark.com/yuque/0/2023/png/614525/1680103092760-027f9cba-b788-498b-a7df-27be3b3bd0c1.png#averageHue=%23f1f0ee&clientId=u80a1aa13-9cf8-4&from=paste&height=539&id=u0dcb05d8&name=image.png&originHeight=674&originWidth=1588&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=253904&status=done&style=none&taskId=uf90e5e5e-32a5-4f06-9226-caa7a612426&title=&width=1270.4)





# 动态 SQL
动态 SQL 是 MyBatis 的强大特性之一。如果你使用过 JDBC 或其它类似的框架，你应该能理解根据不同条件拼接 SQL 语句有多痛苦，例如拼接时要确保不能忘记添加必要的空格，还要注意去掉列表最后一个列名的逗号。利用动态 SQL，可以彻底摆脱这种痛苦。
使用动态 SQL 并非一件易事，但借助可用于任何 SQL 映射语句中的强大的动态 SQL 语言，MyBatis 显著地提升了这一特性的易用性。



## if where

### select

```java
<select id="findActiveBlogLike"
     resultType="Blog">
  SELECT * FROM BLOG
  <where>
    <if test="state != null">
         state = #{state}
    </if>
    <if test="title != null">
        AND title like #{title}
    </if>
    <if test="author != null and author.name != null">
        AND author_name like #{author.name}
    </if>
  </where>
</select>
```



### update

```java
<update id="updateAuthorIfNecessary">
  update Author
    <set>
      <if test="username != null">username=#{username},</if>
      <if test="password != null">password=#{password},</if>
      <if test="email != null">email=#{email},</if>
      <if test="bio != null">bio=#{bio}</if>
    </set>
  where id=#{id}
</update>
```

### foreach

```sql
<select id="selectPostIn" resultType="domain.blog.Post">
  SELECT *
  FROM POST P
  <where>
    <foreach item="item" index="index" collection="list"
        open="ID in (" separator="," close=")" nullable="true">
          #{item}
    </foreach>
  </where>
</select>
```

*foreach* 元素的功能非常强大，它允许你指定一个集合，声明可以在元素体内使用的集合项（item）和索引（index）变量。它也允许你指定开头与结尾的字符串以及集合项迭代之间的分隔符。这个元素也不会错误地添加多余的分隔符，看它多智能！

提示 你可以将任何可迭代对象（如 List、Set 等）、Map 对象或者数组对象作为集合参数传递给 *foreach*。当使用可迭代对象或者数组时，index 是当前迭代的序号，item 的值是本次迭代获取到的元素。当使用 Map 对象（或者 Map.Entry 对象的集合）时，index 是键，item 是值







# Java API

 既然你已经知道如何配置 MyBatis 以及如何创建映射，**是时候来尝点甜头了**。MyBatis 的 Java API 就是这个甜头。稍后你将看到，和 JDBC 相比，MyBatis 大幅简化你的代码并力图保持其简洁、容易理解和维护。为了使得 SQL 映射更加优秀，MyBatis 3 引入了许多重要的改进。  



 当 Mybatis 与一些依赖注入框架（如 Spring 或者 Guice）搭配使用时，**SqlSession 将被依赖注入框架创建并注入，所以你不需要使用 SqlSessionFactoryBuilder 或者** **SqlSessionFactory**，可以直接阅读 SqlSession 这一节。请参考 Mybatis-Spring 或者 Mybatis-Guice 手册以了解更多信息。  



 最后一个 build 方法接受一个 Configuration 实例。Configuration 类包含了对一个 SqlSessionFactory 实例你可能关心的所有内容。在检查配置时，Configuration 类很有用，它允许你查找和操纵 SQL 映射（但当应用开始接收请求时不推荐使用）。你之前学习过的所有配置开关都存在于 Configuration 类，只不过它们是以 Java API 形式暴露的。以下是一个简单的示例，演示如何手动配置 Configuration 实例，然后将它传递给 build() 方法来创建 SqlSessionFactory。  





# 日志

日志的作用：

在打印台输出 sql语句

输出每一步执行了什么东西

把记录写入到 log当中 作为记录

![img](https://cdn.nlark.com/yuque/0/2023/png/614525/1680267541467-69c3d231-a7c3-4e2b-9d32-a818560ba8ff.png)



Mybatis 通过使用内置的日志工厂提供日志功能。内置日志工厂将会把日志工作委托给下面的实现之一：

-  SLF4J 
-  Apache Commons Logging 
-  Log4j 2 
-  Log4j （3.5.9 起废弃） 
-  JDK logging 

MyBatis 内置日志工厂基于运行时自省机制选择合适的日志工具。它会使用第一个查找得到的工具（按上文列举的顺序查找）。如果一个都未找到，日志功能就会被禁用。

不少应用服务器（如 Tomcat 和 WebShpere）的类路径中已经包含 Commons Logging，所以在这种配置环境下的 MyBatis 会把它作为日志工具，记住这点非常重要。这将意味着，在诸如 WebSphere 的环境中，它提供了 Commons Logging 的私有实现，你的 Log4J 配置将被忽略。MyBatis 将你的 Log4J 配置忽略掉是相当令人郁闷的（事实上，正是因为在这种配置环境下，MyBatis 才会选择使用 Commons Logging 而不是 Log4J）。如果你的应用部署在一个类路径已经包含 Commons Logging 的环境中，而你又想使用其它日志工具，你可以通过在 MyBatis 配置文件 mybatis-config.xml 里面添加一项 setting 来选择别的日志工具。



```java
<configuration>
  <settings>
    ...
    <setting name="logImpl" value="LOG4J"/>
    ...
  </settings>
</configuration>
```
