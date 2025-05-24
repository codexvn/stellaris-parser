import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class gete {
    public static void main(String[] args) {
        String te = "    modifier = { add = 1 owner = { num_researched_techs_of_tier = { tier = %s value > %s } } }";
        List<String> r = new ArrayList<>();
        StringJoiner sj = new StringJoiner("\n");
        for (int tier = 1; tier <= 5; tier++) {
            sj.add("""
                    tier%s_count_script_value = {
                        base = 1
                    """.formatted(tier));
            for (int i = 1; i <= 100; i++) {
                sj.add(String.format(te, tier, i));
            }
            sj.add("}");
            r.add(sj.toString());
        }

        System.out.println(r.toString());
    }
}
