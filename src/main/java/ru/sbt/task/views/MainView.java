package ru.sbt.task.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route("main")
@PageTitle("Main")
public class MainView extends VerticalLayout {

    public MainView() {
        add(new H1("Welcome to the Main Page!"));
    }
}
