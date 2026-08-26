package com.easymall.config;

import com.easymall.catalog.*;
import com.easymall.coupon.Coupon;
import com.easymall.coupon.CouponRepository;
import com.easymall.coupon.CouponType;
import com.easymall.user.Role;
import com.easymall.user.User;
import com.easymall.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, CategoryRepository categoryRepository,
                           ProductRepository productRepository, CouponRepository couponRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        if (categoryRepository.count() == 0) seedCatalog();
        if (couponRepository.count() == 0) seedCoupons();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("商城管理员");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        if (!userRepository.existsByUsername("demo")) {
            User demo = new User();
            demo.setUsername("demo");
            demo.setPassword(passwordEncoder.encode("demo123"));
            demo.setNickname("演示用户");
            demo.setRole(Role.USER);
            userRepository.save(demo);
        }
    }

    private void seedCatalog() {
        Category digital = category("数码好物", "⌨", 1);
        Category home = category("生活家居", "⌂", 2);
        Category office = category("办公学习", "✎", 3);
        Category outdoor = category("运动户外", "◒", 4);
        categoryRepository.saveAll(List.of(digital, home, office, outdoor));

        productRepository.saveAll(List.of(
                product(digital, "Wave 机械键盘", "84键热插拔，三模连接", "399.00", 56, 128, "⌨", "mint", "紧凑布局与柔和手感兼得，支持有线、蓝牙与2.4G连接。"),
                product(digital, "AirPods 桌面音箱", "小体积，也有好声场", "269.00", 42, 93, "♫", "sky", "双单元桌面蓝牙音箱，适合宿舍、书桌和小空间。"),
                product(digital, "Flow 无线鼠标", "静音按键，轻盈随行", "129.00", 88, 216, "◉", "lilac", "人体工学曲线设计，长续航双模连接，学习办公都顺手。"),
                product(home, "云朵香薰机", "把森林的呼吸带回家", "159.00", 31, 76, "♨", "peach", "细腻雾化、缺水断电，暖光氛围灯让独处时刻更松弛。"),
                product(home, "晨光保温杯", "12小时锁温，随手一杯暖", "89.00", 120, 341, "♨", "sun", "316不锈钢内胆，轻量杯身，通勤与课堂都方便携带。"),
                product(home, "织梦午睡毯", "柔软亲肤，四季可用", "119.00", 64, 155, "▧", "lilac", "细密针织与耐洗材质，可作午睡毯、披肩或沙发搭毯。"),
                product(office, "Focus 番茄钟", "专注25分钟，从此刻开始", "69.00", 75, 289, "◷", "peach", "旋转即用，无需手机，帮助建立可持续的专注节奏。"),
                product(office, "灵感点阵本", "好想法，值得被认真记下", "35.00", 160, 502, "✎", "sky", "100g护眼纸，180度平摊，适合手账、笔记和项目规划。"),
                product(office, "Halo 阅读灯", "无频闪柔光，照亮深夜书页", "189.00", 48, 118, "☼", "mint", "三档色温无级调光，显色自然，灯臂可灵活调整角度。"),
                product(outdoor, "轻行双肩包", "通勤与周末，一包装下", "239.00", 38, 167, "▰", "sky", "18L轻量设计，独立电脑仓与防泼水面料，简洁耐看。"),
                product(outdoor, "Trail 随行水壶", "轻量耐摔，及时补水", "79.00", 95, 207, "◒", "mint", "食品级材质与一键开盖，防漏提环适合跑步和徒步。"),
                product(outdoor, "自在瑜伽垫", "稳稳支撑每一次伸展", "139.00", 51, 84, "⌁", "peach", "高密度防滑材质，回弹舒适，附带收纳绑带。")
        ));
    }

    private void seedCoupons() {
        LocalDateTime now = LocalDateTime.now();
        couponRepository.saveAll(List.of(
                coupon("NEW20", "新人立减", CouponType.FIXED, "20", "99", now.minusDays(1), now.plusYears(1), 10000),
                coupon("SAVE50", "满减好券", CouponType.FIXED, "50", "299", now.minusDays(1), now.plusYears(1), 5000),
                coupon("ENJOY8", "会员八折", CouponType.PERCENT, "0.80", "499", now.minusDays(1), now.plusYears(1), 1000)
        ));
    }

    private Category category(String name, String icon, int sort) {
        Category category = new Category();
        category.setName(name);
        category.setIcon(icon);
        category.setSortOrder(sort);
        return category;
    }

    private Product product(Category category, String name, String subtitle, String price, int stock,
                            int sales, String icon, String theme, String description) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setSubtitle(subtitle);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setSales(sales);
        product.setIcon(icon);
        product.setTheme(theme);
        product.setDescription(description);
        product.setStatus(ProductStatus.ON_SALE);
        return product;
    }

    private Coupon coupon(String code, String name, CouponType type, String value, String min,
                          LocalDateTime start, LocalDateTime end, int total) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setName(name);
        coupon.setType(type);
        coupon.setDiscountValue(new BigDecimal(value));
        coupon.setMinAmount(new BigDecimal(min));
        coupon.setStartAt(start);
        coupon.setEndAt(end);
        coupon.setTotalQuantity(total);
        coupon.setUsedQuantity(0);
        coupon.setEnabled(true);
        return coupon;
    }
}
