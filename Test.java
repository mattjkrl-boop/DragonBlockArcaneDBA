import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import java.lang.reflect.Method;

public class Test {
    public static void main(String[] args) {
        for (Method m : Item.class.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("attack") || m.getName().toLowerCase().contains("mine")) {
                System.out.println("Item: " + m.toString());
            }
        }
    }
}
