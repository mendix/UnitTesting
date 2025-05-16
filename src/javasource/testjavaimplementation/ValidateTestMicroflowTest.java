package testjavaimplementation;

import org.junit.Test;
import unittesting.TestManager;

import static org.junit.Assert.*;

public class ValidateTestMicroflowTest {
    private final TestManager testManager = TestManager.instance();

    private static final String MODULE = "SuiteForMicroflowValidation";
    private static final String MF_PREFIX_PARAMETER = MODULE + ".UT_TestMicroflow_Parameter_";
    private static final String MF_PREFIX_RETURN_TYPE = MODULE + ".UT_TestMicroflow_ReturnType_";

    @Test
    public void validateTestMicroflowShouldNotAcceptParameterMultiple() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_PARAMETER + "Multiple"));
    }

    @Test
    public void validateTestMicroflowShouldAcceptParameterNone() {
        assertTrue(testManager.validateTestMicroflow(MF_PREFIX_PARAMETER + "None"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptParameterOther() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_PARAMETER + "Other"));
    }

    @Test
    public void validateTestMicroflowShouldAcceptParameterUnitTestContext() {
        assertTrue(testManager.validateTestMicroflow(MF_PREFIX_PARAMETER + "UnitTestContext"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeBinary() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Binary"));
    }

    @Test
    public void validateTestMicroflowShouldAcceptReturnTypeBoolean() {
        assertTrue(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Boolean"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeDateTime() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "DateTime"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeDecimal() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Decimal"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeEnum() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Enum"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeInteger() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Integer"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeList() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "List"));
    }

    @Test
    public void validateTestMicroflowShouldAcceptReturnTypeNothing() {
        assertTrue(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Nothing"));
    }

    @Test
    public void validateTestMicroflowShouldNotAcceptReturnTypeObject() {
        assertFalse(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "Object"));
    }

    @Test
    public void validateTestMicroflowShouldAcceptReturnTypeString() {
        assertTrue(testManager.validateTestMicroflow(MF_PREFIX_RETURN_TYPE + "String"));
    }
}
