package io.axoniq.demo.giftcard.gui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.axoniq.demo.giftcard.api.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ScheduledFuture;

@Push
@Route("")
@PageTitle("Giftcard Browser")
@CssImport("styles/shared-styles.css")
public class NewGiftcardUI extends VerticalLayout {
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    private CardSummaryDataProvider cardSummaryDataProvider;
    private ScheduledFuture<?> updaterThread;
    private final VerticalLayout content;
    private Grid grid;
    //    private Div content;
    private FeederThread thread;
    private Span statusLabel = new Span("");

    public NewGiftcardUI(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.cardSummaryDataProvider = new CardSummaryDataProvider(queryGateway);

        setPadding(false);
        setSpacing(false);
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);

        content = new VerticalLayout();
        content.getStyle().set("flex-grow", "1");
        content.setSpacing(false);

        FlexLayout commandBar = new FlexLayout();
        commandBar.add(createForm(), bulkForm(), redeemForm());

        grid = summaryGrid();
        FlexLayout gridLayout = new FlexLayout();
        gridLayout.setHeightFull();
        gridLayout.add(grid);

        this.content.add(commandBar);
        this.content.add(gridLayout);
        this.content.expand(gridLayout);
        this.content.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);


//        add(header());
        add(this.content);
        add(statusLine());
    }

    protected void onAttach(AttachEvent attachEvent) {
        cardSummaryDataProvider.setUi(attachEvent.getUI());

//        // Start the data feed thread
//        thread = new FeederThread(attachEvent.getUI(), this);
////        thread.start();
//
//        int offset = 0;
//        // offset is in milliseconds
//        ZoneOffset instantOffset = ZoneOffset.ofTotalSeconds(offset / 1000);
//        StatusUpdater statusUpdater = new StatusUpdater(statusLabel, instantOffset);
//        updaterThread = Executors.newScheduledThreadPool(1)
//                .scheduleAtFixedRate(statusUpdater, 1000,
//                        5000, TimeUnit.MILLISECONDS);

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
//        // Cleanup
//        thread.interrupt();
//        thread = null;
    }

    private Component createForm() {
        TextField id = new TextField("Card id");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");
        submit.addClickListener(evt -> {
            logger.info("Create clicked...");
            commandGateway.sendAndWait(new IssueCmd(id.getValue(), Integer.parseInt(amount.getValue())));
            cardSummaryDataProvider.refreshAll();
            Notification.show("Success");
//                        .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
        });

        return new VerticalLayout(id, amount, submit);
    }

    private Component bulkForm() {
        TextField number = new TextField("Number");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");
//        submit.addClickListener(evt -> {
//            commandGateway.sendAndWait(new IssueCmd(id.getValue(), Integer.parseInt(amount.getValue())));
//            Notification.show("Success");
////                        .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
//        });

        return new VerticalLayout(number, amount, submit);
    }

    private Component redeemForm() {
        TextField id = new TextField("Card id");
        TextField amount = new TextField("Amount");
        Button submit = new Button("Submit");
        submit.addClickListener(evt -> {
            logger.info("Redeem clicked...");
            commandGateway.sendAndWait(new RedeemCmd(id.getValue(), Integer.parseInt(amount.getValue())));
//            cardSummaryDataProvider.refreshAll();
            Notification.show("Success");
//                        .addCloseListener(e -> cardSummaryDataProvider.refreshAll());
        });

        return new VerticalLayout(id, amount, submit);
    }

    private Grid summaryGrid() {
        Grid<CardSummary> grid = new Grid<>();
        grid.addColumn(CardSummary::getId).setHeader("Card ID");
        grid.addColumn(CardSummary::getInitialValue).setHeader("Initial value");
        grid.addColumn(CardSummary::getRemainingValue).setHeader("Remaining value");
        grid.setSizeFull();
        grid.setDataProvider(cardSummaryDataProvider);
        return grid;
    }

    private Component header() {
        final Div header = new Div();
        header.getStyle().set("flexShrink", "0");
        header.setText("This is the header. My height is 150 pixels");
        header.setClassName("header");
        header.setHeight("150px");
        header.getStyle().set("border", "1px solid #9E9E9E");
        return header;
    }

    private Component footer() {
        final Div footer = new Div();
        footer.getStyle().set("flexShrink", "0");
        footer.setText("This is the footer area. My height is 100 pixels");
        footer.setClassName("footer");
        footer.setHeight("100px");
        footer.getStyle().set("border", "1px solid #9E9E9E");
        return footer;
    }

    private Component statusLine() {
        final Div footer = new Div();
        footer.getStyle().set("flexShrink", "0");
        footer.setText("footer");
        footer.setHeight("100px");
        footer.add(statusLabel);
        return footer;
    }

    private static class FeederThread extends Thread {
        private final UI ui;
        private final NewGiftcardUI view;

        private int count = 0;

        public FeederThread(UI ui, NewGiftcardUI view) {
            this.ui = ui;
            this.view = view;
        }

        @Override
        public void run() {
            try {
                // Update the data for a while
                while (count < 10) {
                    // Sleep to emulate background work
                    Thread.sleep(500);
                    String message = "This is update " + count++;

                    ui.access(() -> view.add(new Span(message)));
                }

                // Inform that we are done
                ui.access(() -> {
                    view.add(new Span("Done updating"));
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class StatusUpdater implements Runnable {

        private final Span statusLabel;
        private final ZoneOffset instantOffset;

        public StatusUpdater(Span statusLabel, ZoneOffset instantOffset) {
            this.statusLabel = statusLabel;
            this.instantOffset = instantOffset;
        }

        @Override
        public void run() {
            CountCardSummariesQuery query = new CountCardSummariesQuery();
            queryGateway.query(
                    query, CountCardSummariesResponse.class).whenComplete((r, exception) -> {
                if (exception == null) {
                    statusLabel.setText(Instant.ofEpochMilli(r.getLastEvent())
                            .atOffset(instantOffset).toString());
                }
            });
        }
    }

}
