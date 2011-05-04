/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.deployment;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.DefaultButton;
import org.jboss.as.console.client.widgets.DefaultWindow;
import org.jboss.as.console.client.widgets.forms.ComboBoxItem;
import org.jboss.as.console.client.widgets.forms.Form;
import org.jboss.as.console.client.widgets.forms.FormValidation;
import org.jboss.as.console.client.widgets.forms.TextBoxItem;
import org.jboss.as.console.client.widgets.forms.TextItem;

import java.util.List;

/**
 * @author Heiko Braun
 * @author Stan Silvert <ssilvert@redhat.com> (C) 2011 Red Hat Inc.
 * @date 4/8/11
 */
public class DeploymentStep2 {

    private NewDeploymentWizard wizard;
    private DefaultWindow window;
    private List<String> serverGroupNames;

    private Form<DeploymentReference> form;

    public DeploymentStep2(NewDeploymentWizard wizard, DefaultWindow window, List<String> serverGroupNames) {
        this.wizard = wizard;
        this.window = window;
        this.serverGroupNames = serverGroupNames;
    }

    public Widget asWidget()
    {
        VerticalPanel layout = new VerticalPanel();
        layout.getElement().setAttribute("style", "width:95%, margin:15px;");

        layout.add(new HTML("<h3>" + Console.CONSTANTS.common_label_step() + " 2/2: " + 
                                     Console.CONSTANTS.common_label_chooseServerGroup() + "</h3>"));

        form = new Form<DeploymentReference>(DeploymentReference.class);

        TextItem hashField = new TextItem("hash", Console.CONSTANTS.common_label_key());
        TextBoxItem nameField = new TextBoxItem("name", Console.CONSTANTS.common_label_name());
        ComboBoxItem groupSelector = new ComboBoxItem("group", Console.CONSTANTS.common_label_serverGroup());
        groupSelector.setDefaultToFirstOption(true);
        groupSelector.setValueMap(this.serverGroupNames);

        form.setFields(hashField, nameField, groupSelector);

        layout.add(form.asWidget());

        // -----

        Label cancel = new Label(Console.CONSTANTS.common_label_cancel());
        cancel.setStyleName("html-link");
        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                window.hide();
            }
        });

        DefaultButton submit = new DefaultButton(Console.CONSTANTS.common_label_finish(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                FormValidation validation = form.validate();
                if (!validation.hasErrors()) {
                    // proceed
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
                    {
                        @Override
                        public void execute() {
                            wizard.onDeployToGroup(form.getUpdatedEntity());
                        }
                    });

                }
            }
        });

        HorizontalPanel options = new HorizontalPanel();
        options.getElement().setAttribute("style", "margin-top:10px;width:100%");

        HTML spacer = new HTML("&nbsp;");
        options.add(spacer);

        options.add(submit);
        options.add(spacer);
        options.add(cancel);
        cancel.getElement().getParentElement().setAttribute("style", "vertical-align:middle");
        submit.getElement().getParentElement().setAttribute("align", "right");
        submit.getElement().getParentElement().setAttribute("width", "100%");

        layout.add(options);

        return layout;
    }


    void edit(DeploymentReference ref)
    {
        form.edit(ref);
    }
}
