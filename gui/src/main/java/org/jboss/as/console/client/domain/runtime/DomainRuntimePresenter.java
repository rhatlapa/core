package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.UseGatekeeper;
import com.gwtplatform.mvp.client.proxy.*;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshHosts;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;
import org.jboss.gwt.flow.client.*;

import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class DomainRuntimePresenter
        extends PerspectivePresenter<DomainRuntimePresenter.MyView, DomainRuntimePresenter.MyProxy>
        implements UnauthorizedEvent.UnauthorizedHandler {

    @ProxyCodeSplit
    @NameToken(NameTokens.DomainRuntimePresenter)
    @UseGatekeeper(DomainRuntimegateKeeper.class)
    public interface MyProxy extends Proxy<DomainRuntimePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DomainRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);

        void setTopology(String selectedHost, String selectedServer, HostStore.Topology topology);
    }


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();


    private final Dispatcher circuit;
    private final ServerStore serverStore;
    private HandlerRegistration handlerRegistration;
    private final HostStore hostStore;
    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final ServerGroupStore serverGroupStore;

    @Inject
    public DomainRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            HostStore hostStore, SubsystemLoader subsysStore,
            ServerGroupStore serverGroupStore, Header header, UnauthorisedPresenter unauthorisedPresenter,
            Dispatcher circuit, ServerStore serverStore) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.DomainRuntimePresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.placeManager = placeManager;

        this.hostStore = hostStore;
        this.subsysStore = subsysStore;
        this.serverGroupStore = serverGroupStore;
        this.circuit = circuit;
        this.serverStore = serverStore;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        handlerRegistration = hostStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {

                if (!isVisible()) return;

                // server picker update
                if (hostStore.hasSelectedServer()) {
                    getView().setTopology(hostStore.getSelectedHost(), hostStore.getSelectedServer(), hostStore.getTopology());
                }

                // subsystem tree update
                if (hostStore.hasSelectedServer())
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            loadSubsystems();
                        }
                    });

                else
                    getView().setSubsystems(Collections.EMPTY_LIST);
            }
        });

    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
        }
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                // load host and server data
                circuit.dispatch(new RefreshHosts());
            }
        });
    }


    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault) {
        if(revealDefault)
        {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.HostVMMetricPresenter).build());
        }
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    private void loadSubsystems() {
        // clear view
        getView().setSubsystems(Collections.<SubsystemRecord>emptyList());

        final Function<FunctionContext> f2 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                final String serverSelection = hostStore.getSelectedServer();
                ServerInstance server = serverStore.getServerInstance(serverSelection);
                serverGroupStore.loadServerGroup(server.getGroup(), new PushFlowCallback<ServerGroupRecord>(control));
            }
        };
        final Function<FunctionContext> f3 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                ServerGroupRecord group = control.getContext().pop();
                subsysStore.loadSubsystems(group.getProfileName(),
                        new PushFlowCallback<List<SubsystemRecord>>(control));
            }
        };
        final Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                // TODO i18n
                Console.error("Cannot load subsystems of selected server");
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                List<SubsystemRecord> subsystems = context.pop();
                getView().setSubsystems(subsystems);
            }
        };

        // load subsystems for selected server
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome, f2, f3);
            }
        });

    }
}
