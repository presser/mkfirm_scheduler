import com.google.common.base.Preconditions;

import java.util.Random;

/**
 * Helper class to generate random intervals, used in tests
 */
public class RandomIntervalGenerator
{
    private int min;
    private int max;
    private Random random;

    public RandomIntervalGenerator(int min, int max)
    {
        Preconditions.checkArgument(min <= max);
        this.min = min;
        this.max = max;
        this.random = new Random();
    }

    public int nextInterval()
    {
        return min == max ? min : min + random.nextInt(max - min);
    }
}
