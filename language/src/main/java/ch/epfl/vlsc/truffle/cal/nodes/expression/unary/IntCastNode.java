package ch.epfl.vlsc.truffle.cal.nodes.expression.unary;

import ch.epfl.vlsc.truffle.cal.CALException;
import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALBigNumber;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigInteger;
import java.rmi.UnexpectedException;

import static java.lang.Math.pow;

// TODO: Huge potential for optimization
public class IntCastNode extends CALExpressionNode {
    @Child private CALExpressionNode intsizeNode;
    @Child private CALExpressionNode valueNode;
    private boolean signed;

    public IntCastNode(CALExpressionNode intsize, CALExpressionNode value, boolean signed) {
        super();
        this.intsizeNode = intsize;
        this.valueNode = value;
        this.signed = signed;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object intsizevalobj = intsizeNode.executeGeneric(frame);
        Integer intsizeVal = null;
        if(intsizevalobj instanceof  CALBigNumber){
            intsizeVal = ((CALBigNumber) intsizevalobj).getValue().intValue();
        } else if (intsizevalobj instanceof Long){
            intsizeVal = ((Long) intsizevalobj).intValue();
        } else
            throw new CALException("Unexpected type: " + intsizevalobj.getClass().getName() + ", where number type expected", this);


        Object valueObj = valueNode.executeGeneric(frame);
        BigInteger value = null;
        if(valueObj instanceof CALBigNumber) {
            value = ((CALBigNumber) valueObj).getValue();
        } else if(valueObj instanceof Long) {
            value = new BigInteger(String.valueOf((Long) valueObj));
        } else {
            // TODO : Once lexical scoping is fixed, this should be unreachable code and should result in a runtime exception
            // FIXME: The return of valueObj is a temporary fix to lack of proper lexical scoping
            return valueObj;
        }

        if(intsizeVal <= 0) return new CALBigNumber(value);

        BigInteger NEGONE = new BigInteger("-1");

        BigInteger max;
        BigInteger min;
        if(!signed) {
            min = BigInteger.ZERO;
            max = BigInteger.ONE.shiftLeft(intsizeVal).subtract(BigInteger.ONE);
        } else {
            max = (BigInteger.ONE).shiftLeft(intsizeVal - 1);
            min = max.multiply(NEGONE);
            max = max.subtract(BigInteger.ONE);
        }

        int maxcompval = value.compareTo(max);
        int mincompval = value.compareTo(min);
        if (maxcompval > 0) {
            while(maxcompval > 0) {
                value = min.add(value.subtract(max.add(BigInteger.ONE)));
                maxcompval = value.compareTo(max);
            }
        } else if (mincompval < 0) {
            while(mincompval < 0) {
                value = max.subtract(value.multiply(NEGONE).subtract(min.multiply(NEGONE).add(BigInteger.ONE)));
                mincompval = value.compareTo(min);
            }
        }

        return new CALBigNumber(value);
    }
}
