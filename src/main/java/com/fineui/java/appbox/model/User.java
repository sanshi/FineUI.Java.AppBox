package com.fineui.java.appbox.model;

import com.fineui.java.binding.Display;
import com.fineui.java.binding.DisplayFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户。与角色、职称多对多（本实体是两张联接表 {@code role_users} / {@code title_users} 的维护方），所属部门多对一。
 *
 * <p>{@code deptId} 是真实外键列（表单下拉框直接绑定它），{@code dept} 是只读导航（列表列 {@code deptName} 由它派生）。
 * {@code password} 存的是 BCrypt 哈希、从不存明文。{@code @Display} 驱动表格列头与表单字段标题；
 * {@code @NotBlank} 等校验注解由 {@code getModelState()} 在绑定后执行。
 */
@Entity
@Table(name = "users")
public class User implements KeyId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Display(name = "用户名")
    @NotBlank(message = "用户名不能为空！")
    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Display(name = "邮箱")
    @NotBlank(message = "邮箱不能为空！")
    @Column(length = 100, nullable = false)
    private String email;

    @Display(name = "密码")
    @Column(length = 100, nullable = false)
    private String password;

    @Display(name = "启用")
    @Column(nullable = false)
    private boolean enabled;

    @Display(name = "性别")
    @NotBlank(message = "性别不能为空！")
    @Column(length = 10, nullable = false)
    private String gender;

    @Display(name = "中文名")
    @Column(length = 100)
    private String chineseName;

    @Display(name = "英文名")
    @Column(length = 100)
    private String englishName;

    @Display(name = "照片")
    @Column(length = 200)
    private String photo;

    @Display(name = "QQ")
    @Column(length = 50)
    private String qq;

    @Display(name = "公司邮箱")
    @Column(length = 100)
    private String companyEmail;

    @Display(name = "工作电话")
    @Column(length = 50)
    private String officePhone;

    @Display(name = "分机号")
    @Column(length = 50)
    private String officePhoneExt;

    @Display(name = "家庭电话")
    @Column(length = 50)
    private String homePhone;

    @Display(name = "手机号")
    @Column(length = 50)
    private String cellPhone;

    @Display(name = "地址")
    @Column(length = 500)
    private String address;

    @Display(name = "备注")
    @Column(length = 500)
    private String remark;

    @Display(name = "身份证")
    @Column(length = 50)
    private String identityCard;

    @Display(name = "生日")
    @DisplayFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Display(name = "任职时间")
    @DisplayFormat(pattern = "yyyy-MM-dd")
    private LocalDate takeOfficeTime;

    @Display(name = "上次登录时间")
    @DisplayFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Display(name = "创建时间")
    @DisplayFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Display(name = "所属角色")
    @ManyToMany
    @JoinTable(name = "role_users",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new ArrayList<>();

    @Display(name = "拥有职称")
    @ManyToMany
    @JoinTable(name = "title_users",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "title_id"))
    private List<Title> titles = new ArrayList<>();

    @Display(name = "所属部门")
    @Column(name = "dept_id")
    private Integer deptId;

    @ManyToOne
    @JoinColumn(name = "dept_id", insertable = false, updatable = false)
    private Dept dept;

    /** 所属部门名称（由只读导航派生，供表格列直接绑定）。 */
    @Display(name = "所属部门")
    public String getDeptName() {
        return dept == null ? null : dept.getName();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getOfficePhone() {
        return officePhone;
    }

    public void setOfficePhone(String officePhone) {
        this.officePhone = officePhone;
    }

    public String getOfficePhoneExt() {
        return officePhoneExt;
    }

    public void setOfficePhoneExt(String officePhoneExt) {
        this.officePhoneExt = officePhoneExt;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public void setHomePhone(String homePhone) {
        this.homePhone = homePhone;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getIdentityCard() {
        return identityCard;
    }

    public void setIdentityCard(String identityCard) {
        this.identityCard = identityCard;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDate getTakeOfficeTime() {
        return takeOfficeTime;
    }

    public void setTakeOfficeTime(LocalDate takeOfficeTime) {
        this.takeOfficeTime = takeOfficeTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<Title> getTitles() {
        return titles;
    }

    public void setTitles(List<Title> titles) {
        this.titles = titles;
    }

    public Integer getDeptId() {
        return deptId;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public Dept getDept() {
        return dept;
    }

    public void setDept(Dept dept) {
        this.dept = dept;
    }
}
