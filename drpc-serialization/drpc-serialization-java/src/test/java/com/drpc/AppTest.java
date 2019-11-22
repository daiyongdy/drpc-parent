package com.drpc;

import com.drpc.serialization.hessian.HessianSerializer;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void testSerialization() throws Exception {
        ResultWrapper resultWrapper = new ResultWrapper();
        resultWrapper.setResult("daiyong");
        resultWrapper.setException(new RuntimeException("test error"));

        byte[] bytes = new HessianSerializer().writeObject(resultWrapper);

        ResultWrapper resultWrapper1 = new HessianSerializer().readObject(bytes, ResultWrapper.class);
        System.out.println(resultWrapper1.getResult());
        if (resultWrapper1.getException() != null) {
            throw resultWrapper1.getException();
        }
    }
}
