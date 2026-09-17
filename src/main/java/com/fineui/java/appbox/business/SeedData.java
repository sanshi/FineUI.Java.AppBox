package com.fineui.java.appbox.business;

import com.fineui.java.appbox.model.Config;
import com.fineui.java.appbox.model.Dept;
import com.fineui.java.appbox.model.Role;
import com.fineui.java.appbox.model.Title;
import com.fineui.java.appbox.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 配置 / 部门 / 用户 / 角色 / 职称的种子数据（权限见 {@link SeedPowers}，菜单见 {@link SeedMenus}）。
 * 演示账号 user0、user2、…（用户名即密码）约 200 个，外加超级管理员 admin/admin。
 */
final class SeedData {

    private SeedData() {
    }

    /** 性别与中文名交替排列：偶数位是性别、奇数位是姓名。 */
    private static final String[] USER_NAMES = { "男", "童光喜", "男", "方原柏", "女", "祝春亚", "男", "涂辉", "男", "舒兆国", "男", "熊忠文", "男", "徐吉琳", "男", "方金海", "男", "包卫峰", "女", "靖小燕", "男", "杨习斌", "男", "徐长旺", "男", "聂建雄", "男", "周敦友", "男", "陈友庭", "女", "陆静芳", "男", "袁国柱", "女", "骆新桂", "男", "许治国", "男", "马先加", "男", "赵恢川", "男", "柯常胜", "男", "黄国鹏", "男", "柯尊北", "男", "刘海云", "男", "罗清波", "男", "张业权", "女", "丁溯鋆", "男", "吴俊", "男", "郑江", "男", "李亚华", "男", "石光富", "男", "谭志洪", "男", "胡中生", "男", "董龙剑", "男", "陈红", "男", "汪海平", "男", "彭道洲", "女", "尹莉君", "男", "占耀玲", "男", "付杰", "男", "王红艳", "男", "邝兴", "男", "饶玮", "男", "王方胜", "男", "陈劲松", "男", "邓庆华", "男", "王石林", "男", "胡俊明", "男", "索相龙", "男", "陈海军", "男", "吴文涛", "女", "熊望梅", "女", "段丽华", "女", "胡莎莎", "男", "徐友安", "男", "肖诗涛", "男", "王闯", "男", "余兴龙", "男", "芦荫杰", "男", "丁金富", "男", "谭军令", "女", "鄢旭燕", "男", "田坤", "男", "夏德胜", "男", "喻显发", "男", "马兴宝", "男", "孙学涛", "男", "陶云成", "男", "马远健", "男", "田华", "男", "聂子森", "男", "郑永军", "男", "余昌平", "男", "陶俊华", "男", "李小林", "男", "李荣宝", "男", "梅盈凯", "男", "张元群", "男", "郝新华", "男", "刘红涛", "男", "向志强", "男", "伍小峰", "男", "胡勇民", "男", "黄定祥", "女", "高红香", "男", "刘军", "男", "叶松", "男", "易俊林", "男", "张威", "男", "刘卫华", "男", "李浩", "男", "李寿庚", "男", "涂洋", "男", "曹晶", "男", "陈辉", "女", "彭博", "男", "严雪冰", "男", "刘青", "女", "印媛", "男", "吴道雄", "男", "邓旻", "男", "陈骏", "男", "崔波", "男", "韩静颐", "男", "严安勇", "男", "刘攀", "女", "刘艳", "女", "孙昕", "女", "郑新", "女", "徐睿", "女", "李月杰", "男", "吕焱鑫", "女", "刘沈", "男", "朱绍军", "女", "马茜", "女", "唐蕾", "女", "刘姣", "女", "于芳", "男", "吴健", "女", "张丹梅", "女", "王燕", "女", "贾兆梅", "男", "程柏漠", "男", "程辉", "女", "任明慧", "女", "焦莹", "女", "马淑娟", "男", "徐涛", "男", "孙庆国", "男", "刘胜", "女", "傅广凤", "男", "袁弘", "男", "高令旭", "男", "栾树权", "女", "申霞", "女", "韩文萍", "女", "隋艳", "男", "邢海洲", "女", "王宁", "女", "陈晶", "女", "吕翠", "女", "刘少敏", "女", "刘少君", "男", "孔鹏", "女", "张冰", "女", "王芳", "男", "万世忠", "女", "徐凡", "女", "张玉梅", "女", "何莉", "女", "时会云", "女", "王玉杰", "女", "谭素英", "女", "李艳红", "女", "刘素莉", "男", "王旭海", "女", "安丽梅", "女", "姚露", "女", "贾颖", "女", "曹微", "男", "黄经华", "女", "陈玉华", "女", "姜媛", "女", "魏立平", "女", "张萍", "男", "来辉", "女", "陈秀玫", "男", "石岩", "男", "王洪捍", "男", "张树军", "女", "李亚琴", "女", "王凤", "女", "王珊华", "女", "杨丹丹", "女", "教黎明", "女", "修晶", "女", "丁晓霞", "女", "张丽", "女", "郭素兰", "女", "徐艳丽", "女", "任子英", "女", "胡雁", "女", "彭洪亮", "女", "高玉珍", "女", "王玉姝", "男", "郑伟", "女", "姜春玲", "女", "张伟", "女", "王颖", "女", "金萍", "男", "孙望", "男", "闫宝东", "男", "周相永", "女", "杨美娜", "女", "欧立新", "女", "刘宝霞", "女", "刘艳杰", "女", "宋艳平", "男", "李克", "女", "梁翠", "女", "宗宏伟", "女", "刘国伟", "女", "敖志敏", "女", "尹玲" };

    private static final String[] EMAIL_NAMES = { "qq.com", "gmail.com", "163.com", "126.com", "outlook.com", "foxmail.com" };

    static List<Config> configs() {
        List<Config> list = new ArrayList<>();
        list.add(config("Title", "FineUI.Java.AppBox", "网站的标题"));
        list.add(config("PageSize", "20", "表格每页显示的个数"));
        list.add(config("Theme", "Cupertino", "网站主题"));
        list.add(config("HelpList",
                "[{\"Text\":\"万年历\",\"Icon\":\"Calendar\",\"ID\":\"wannianli\",\"URL\":\"/help/wannianli.html\"},"
                        + "{\"Text\":\"科学计算器\",\"Icon\":\"Calculator\",\"ID\":\"jisuanqi\",\"URL\":\"/help/jisuanqi.html\"},"
                        + "{\"Text\":\"系统帮助\",\"Icon\":\"Help\",\"ID\":\"help\",\"URL\":\"/admin/help\"}]",
                "帮助下拉列表的JSON字符串"));
        return list;
    }

    private static Config config(String key, String value, String remark) {
        Config c = new Config();
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setRemark(remark);
        return c;
    }

    /** 部门树：节点的 children 只在种子阶段临时承载子节点，由初始化器递归入库。 */
    static List<Dept> depts() {
        List<Dept> roots = new ArrayList<>();
        roots.add(dept("研发部", 1, "顶级部门",
                dept("开发部", 1, "二级部门"),
                dept("测试部", 2, "二级部门")));
        roots.add(dept("销售部", 2, "顶级部门",
                dept("直销部", 1, "二级部门"),
                dept("渠道部", 2, "二级部门")));
        roots.add(dept("客服部", 3, "顶级部门",
                dept("实施部", 1, "二级部门"),
                dept("售后服务部", 2, "二级部门"),
                dept("大客户服务部", 3, "二级部门")));
        roots.add(dept("财务部", 4, "顶级部门"));
        roots.add(dept("行政部", 5, "顶级部门",
                dept("人事部", 1, "二级部门"),
                dept("后勤部", 2, "二级部门"),
                dept("运输部", 3, "二级部门",
                        dept("省内运输部", 1, "三级部门"),
                        dept("国内运输部", 2, "三级部门"),
                        dept("国际运输部", 3, "三级部门"))));
        return roots;
    }

    private static Dept dept(String name, int sortIndex, String remark, Dept... children) {
        Dept d = new Dept();
        d.setName(name);
        d.setSortIndex(sortIndex);
        d.setRemark(remark);
        d.setChildren(new ArrayList<>(Arrays.asList(children)));
        return d;
    }

    static List<User> users() {
        List<User> users = new ArrayList<>();
        Random rdm = new Random();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0, count = USER_NAMES.length; i < count; i += 2) {
            String gender = USER_NAMES[i];
            String chineseName = USER_NAMES[i + 1];
            String userName = "user" + i;
            User u = new User();
            u.setName(userName);
            u.setGender(gender);
            u.setPassword(PasswordUtil.createSeedPassword(userName));
            u.setChineseName(chineseName);
            u.setEmail(userName + "@" + EMAIL_NAMES[rdm.nextInt(EMAIL_NAMES.length)]);
            u.setEnabled(true);
            u.setCreateTime(now);
            users.add(u);
        }
        User admin = new User();
        admin.setName("admin");
        admin.setGender("男");
        admin.setPassword(PasswordUtil.createSeedPassword("admin"));
        admin.setChineseName("超级管理员");
        admin.setEmail("admin@fineui.com");
        admin.setEnabled(true);
        admin.setCreateTime(now);
        users.add(admin);
        return users;
    }

    static List<Role> roles() {
        List<Role> list = new ArrayList<>();
        for (String name : new String[] { "系统管理员", "部门管理员", "项目经理", "开发经理", "开发人员", "后勤人员", "外包人员" }) {
            Role r = new Role();
            r.setName(name);
            r.setRemark("");
            list.add(r);
        }
        return list;
    }

    static List<Title> titles() {
        List<Title> list = new ArrayList<>();
        for (String name : new String[] { "总经理", "部门经理", "高级工程师", "工程师" }) {
            Title t = new Title();
            t.setName(name);
            list.add(t);
        }
        return list;
    }
}
