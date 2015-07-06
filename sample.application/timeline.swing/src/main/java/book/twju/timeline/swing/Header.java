package book.twju.timeline.swing;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import book.twju.timeline.model.Item;
import book.twju.timeline.model.Timeline;

class Header<T extends Item> {
  
  private static final String TITLE = "Timeline";

  private final BackgroundProcessor backgroundProcessor;
  private final Timeline<T> timeline;

  JPanel component;
  JButton fetchNew;
  JLabel title;
  
  Header( Timeline<T> timeline ) {
    this( timeline, new BackgroundProcessor() );
  }

  Header( Timeline<T> timeline, BackgroundProcessor backgroundProcessor ) {
    this.backgroundProcessor = backgroundProcessor;
    this.timeline = timeline;
  }

  void createUi() {
    createComponent();
    createTitle();
    createFetchNew();
    layout();
  }
  
  Component getComponent() {
    return component;
  }

  void update() {
    backgroundProcessor.process( () -> {
      int count = timeline.getNewCount();
      backgroundProcessor.dispatchToUiThread( () -> update( count ) );
    } );
  }
  
  void onFetchNew( ActionListener listener ) {
    fetchNew.addActionListener( evt -> notifyAboutFetchRequest( listener, evt ) );
  }
  
  private void update( int count ) {
    fetchNew.setText( count + " new" );
    fetchNew.setVisible( count > 0 );
  }
  
  private void notifyAboutFetchRequest( ActionListener listener, ActionEvent evt ) {
    listener.actionPerformed( new ActionEvent( component, evt.getID(), evt.getActionCommand() ) );
    fetchNew.setVisible( false );
  }
  
  private void createComponent() {
    component = new JPanel();
  }
  
  private void createTitle() {
    title = new JLabel( TITLE );
    Font baseFont = title.getFont();
    title.setFont( new Font( baseFont.getName(), baseFont.getStyle(), baseFont.getSize() + 10 ) );
  }

  private void createFetchNew() {
    fetchNew = new JButton();
    fetchNew.setVisible( false );
  }
  
  private void layout() {
    component.setLayout( new FlowLayout( FlowLayout.CENTER, 25, 10 ) );
    component.add( title );
    component.add( fetchNew );
  }
}