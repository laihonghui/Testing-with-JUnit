package book.twju.timeline.swing;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JScrollPane;

import book.twju.timeline.model.Item;
import book.twju.timeline.model.Timeline;
import book.twju.timeline.ui.ItemUiMap;

class TopItemScroller<T extends Item> {
  
  static final int TOP_POSITION = 5;
  
  private final UiThreadDispatcher uiThreadDispatcher;
  private final ItemUiMap<T, Container> itemUiMap;
  private final Timeline<T> timeline;
  private final ItemUiList<T> itemUiList;

  TopItemScroller( Timeline<T> timeline, ItemUiMap<T, Container> itemUiMap, ItemUiList<T> itemUiList ) {
    this( timeline, itemUiMap, itemUiList, new UiThreadDispatcher() );
  }
  
  TopItemScroller(
    Timeline<T> timeline, ItemUiMap<T, Container> itemUiMap, ItemUiList<T> itemUiList, UiThreadDispatcher dispatcher )
  {
    this.uiThreadDispatcher = dispatcher;
    this.itemUiList = itemUiList;
    this.itemUiMap = itemUiMap;
    this.timeline = timeline;
  }

  public void scrollIntoView() {
    uiThreadDispatcher.dispatch( () -> doScrollIntoView() );
  }
  
  public void doScrollIntoView() {
    if( timeline.getTopItem().isPresent() ) {
      SwingItemUi<T> itemUi = getItemUi();
      if( itemUi != null ) {
        Component component = itemUi.getComponent();
        if( component.isShowing() ) {
          updateVerticalScrollbarSelection( component );
        }
      }
    }
  }
  
  void setScrollbarSelection( int newValue ) {
    getContentPane().getVerticalScrollBar().getModel().setValue( newValue );
  }

  private SwingItemUi<T> getItemUi() {
    T item = timeline.getTopItem().get();
    return ( SwingItemUi<T> )itemUiMap.findByItemId( item.getId() );
  }

  private void updateVerticalScrollbarSelection( Component component ) {
    double verticalLocation = computeVerticalLocation( component );
    if( verticalLocation < TOP_POSITION ) {
      updateVerticalScrollbarSelection( verticalLocation );
    }
  }

  private void updateVerticalScrollbarSelection( double verticalLocation ) {
    int oldValue = getContentPane().getVerticalScrollBar().getModel().getValue();
    setScrollbarSelection( oldValue - 15 + ( int )verticalLocation );
  }

  private double computeVerticalLocation( Component component ) {
    double y1 = component.getParent().getParent().getLocationOnScreen().getY();
    double y2 = component.getLocationOnScreen().getY();
    return y2 - y1;
  }

  private JScrollPane getContentPane() {
    return ( JScrollPane )itemUiList.getComponent();
  }
}