package org.renjin.gcc.codegen.fatptr;

import com.google.common.collect.Lists;
import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.ParamStrategy;
import org.renjin.gcc.codegen.var.Value;
import org.renjin.gcc.codegen.var.Var;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleParameter;

import java.util.List;

/**
 * Strategy for using FatPtrs as a single wrapped fat pointer
 */
public class WrappedFatPtrParamStrategy implements ParamStrategy {

  private ValueFunction valueFunction;

  public WrappedFatPtrParamStrategy(ValueFunction valueFunction) {
    this.valueFunction = valueFunction;
  }

  @Override
  public List<Type> getParameterTypes() {
    return Lists.newArrayList(FatPtrStrategy.wrapperType(valueFunction.getValueType()));
  }

  @Override
  public ExprGenerator emitInitialization(MethodGenerator mv, GimpleParameter parameter, List<Var> paramVars, VarAllocator localVars) {
    Var array = localVars.reserveArrayRef(parameter.getName() + "$array", valueFunction.getValueType());
    Var offset = localVars.reserveInt(parameter.getName() + "$offset");

    Value wrapper = paramVars.get(0);
    
    array.store(mv, Wrappers.getArray(wrapper));
    offset.store(mv, Wrappers.getOffset(wrapper));
    
    return new FatPtrExpr(array, offset, valueFunction);
  }

  @Override
  public void emitPushParameter(MethodGenerator mv, ExprGenerator parameterValueGenerator) {
    FatPtrExpr fatPtrExpr = (FatPtrExpr) parameterValueGenerator;
    fatPtrExpr.wrap().load(mv);
  }
}
