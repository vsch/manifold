package manifold.api.json;

import java.io.StringReader;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptException;
import manifold.util.Pair;

public class DefaultParser implements IJsonParser
{
  private static final DefaultParser INSTANCE = new DefaultParser();

  public static IJsonParser instance()
  {
    return INSTANCE;
  }

  @Override
  public Bindings parseJson( String jsonText, boolean withBigNumbers, boolean withTokens ) throws ScriptException
  {
    SimpleParserImpl parser = new SimpleParserImpl( new Tokenizer( new StringReader( jsonText ) ), withBigNumbers );
    Object result = parser.parse( withTokens );
    List<String> errors = parser.getErrors();
    if( errors.size() != 0 )
    {
      StringBuilder sb = new StringBuilder( "Found errors:\n" );
      for( String err : errors )
      {
        sb.append( err ).append( "\n" );
      }
      throw new ScriptException( sb.toString() );
    }
    if( result instanceof Pair )
    {
      result = ((Pair)result).getSecond();
    }
    if( result instanceof Bindings )
    {
      return (Bindings)result;
    }
    return NashornJsonParser.wrapValueInBindings( result );
  }
}
