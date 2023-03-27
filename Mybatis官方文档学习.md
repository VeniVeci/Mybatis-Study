

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


 你可以重写已有的类型处理器或创建你自己的类型处理器来处理不支持的或非标准的类型。 具体做法为：实现 org.apache.ibatis.type.TypeHandler 接口， 或继承一个很便利的类 org.apache.ibatis.type.BaseTypeHandler， 并且可以（可选地）将它映射到一个 JDBC 类型。比如：  

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
 使用上述的类型处理器将会覆盖已有的处理 Java String 类型的属性以及 VARCHAR 类型的参数和结果的类型处理器。 要注意 MyBatis 不会通过检测数据库元信息来决定使用哪种类型，所以你必须在参数和结果映射中指明字段是 VARCHAR 类型， 以使其能够绑定到正确的类型处理器上。这是因为 MyBatis 直到语句被执行时才清楚数据类型。  
