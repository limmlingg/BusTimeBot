package unit;

import org.junit.Assert;
import org.junit.Test;

import model.BusType;
import model.BusType.Type;

public class BusTypeTest {

    @Test
    public void testBusTypeCreation() {
        BusType type = new BusType();
        boolean result = type.setTrue(Type.PUBLIC);

        Assert.assertTrue(result);
        Assert.assertTrue(type.isType(Type.PUBLIC));
        Assert.assertFalse(type.isType(Type.NUS));
        Assert.assertFalse(type.isType(Type.NTU));
    }

    @Test
    public void testNullType() {
        BusType type = new BusType();
        boolean result = type.setTrue(null);

        Assert.assertFalse(result);
        Assert.assertFalse(type.isType(Type.PUBLIC));
        Assert.assertFalse(type.isType(Type.NUS));
        Assert.assertFalse(type.isType(Type.NTU));
    }

    @Test
    public void testToString() {
        String answer = "{PUBLIC=false, NUS=true, NTU=false}";
        BusType type = new BusType();
        type.setTrue(Type.NUS);
        Assert.assertTrue(answer.equals(type.toString()));
    }

}
