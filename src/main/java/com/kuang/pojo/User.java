package com.kuang.pojo;

/**
 * @author Zhangxuhui
 * @Description
 * @create 2021/4/14 - 11:58
 */
//@AllArgsConstructor
//@NoArgsConstructor
//@Setter
//@Getter
//@ToString
public class User {

    private int id;  //id
    private String name;   //姓名
    private String pwd;   //密码


    //构造,有参,无参

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public User(int id, String name, String pwd) {
        this.id = id;
        this.name = name;
        this.pwd = pwd;
    }

    public User() {
    }
//set/get
    //toString()
}
