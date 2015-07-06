package book.twju.timeline.swing;

import static book.twju.timeline.swing.ItemViewerCompound.ITEM_UI_FACTORY_MUST_NOT_BE_NULL;
import static book.twju.timeline.swing.ItemViewerCompound.TIMELINE_MUST_NOT_BE_NULL;
import static book.twju.timeline.test.util.ThrowableCaptor.thrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.awt.Container;

import org.junit.Before;
import org.junit.Test;

import book.twju.timeline.model.Item;
import book.twju.timeline.model.Timeline;
import book.twju.timeline.swing.ItemViewerCompound;
import book.twju.timeline.ui.ItemUiFactory;

public class ItemViewerCompoundTest {

  private ItemUiFactory<Item, Container> itemUiFactory;
  private ItemViewerCompound<Item> compound;
  private Timeline<Item> timeline;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    timeline = mock( Timeline.class );
    itemUiFactory = mock( ItemUiFactory.class );
    compound = new ItemViewerCompound<>( timeline, itemUiFactory );
  }
  
  @Test
  public void getTopItemUpdater() {
    assertThat( compound.getTopItemUpdater() ).isNotNull();
  }

  @Test
  public void getScroller() {
    assertThat( compound.getScroller() ).isNotNull();
  }

  @Test
  public void getItemUiList() {
    assertThat( compound.getItemUiList() ).isNotNull();
  }
  
  @Test
  public void constructWithNullAsTimeline() {
    Throwable actual = thrownBy( () -> new ItemViewerCompound<>( null, itemUiFactory ) );
    
    assertThat( actual )
      .hasMessage( TIMELINE_MUST_NOT_BE_NULL )
      .isInstanceOf( IllegalArgumentException.class );
  }
  
  @Test
  public void constructWithNullAsItemUiFactory() {
    Throwable actual = thrownBy( () -> new ItemViewerCompound<>( timeline, null ) );
    
    assertThat( actual )
      .hasMessage( ITEM_UI_FACTORY_MUST_NOT_BE_NULL )
      .isInstanceOf( IllegalArgumentException.class );
  }
}