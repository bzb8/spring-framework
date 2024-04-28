import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;

public class WildcardTypeTest {

    // 指定上界 Number，下边界默认为 []
    private List<? extends Number> a;
    // 指定下界 String，上边界默认是 Object
    private List<? super String> b;
    // 上界和下界都不指定，上边界默认是 Object，下边界默认为 []
    private Class<?> clazz;

    // 没有通配符，不是 WildcardType
    private List<String> c;

    @Test
    public void test() throws Exception {
        Field[] fields = WildcardTypeTest.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Type type = field.getGenericType();
            String nameString = field.getName();
            //1. 先拿到范型类型
            if (!(type instanceof ParameterizedType)) {
                continue;
            }

            //2. 再从范型里拿到通配符类型
            ParameterizedType parameterizedType = (ParameterizedType) type;
            type = parameterizedType.getActualTypeArguments()[0];
            if (!(type instanceof WildcardType)) {
                continue;
            }

            System.out.println("-------------" + nameString + "--------------");
            WildcardType wildcardType = (WildcardType) type;
            Type[] lowerTypes = wildcardType.getLowerBounds();
            if (lowerTypes != null) {
                System.out.println("下边界：" + Arrays.toString(lowerTypes));
            }
            Type[] upTypes = wildcardType.getUpperBounds();
            if (upTypes != null) {
                System.out.println("上边界：" + Arrays.toString(upTypes));
            }
        }
    }
}
