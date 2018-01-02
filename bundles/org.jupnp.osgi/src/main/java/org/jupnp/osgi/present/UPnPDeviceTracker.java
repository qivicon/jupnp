/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.osgi.present;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jupnp.UpnpService;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 */
class UPnPDeviceTracker extends ServiceTracker {

    private final Logger log = LoggerFactory.getLogger(UPnPDeviceTracker.class);

    private UpnpService upnpService;
    private Map<UPnPDevice, LocalDevice> registrations = new Hashtable<UPnPDevice, LocalDevice>();

    public UPnPDeviceTracker(BundleContext context, UpnpService upnpService, Filter filter) {
        super(context, filter, null);
		log.trace("ENTRY {}.{}: {} {} {}", this.getClass().getName(), "<init>", context, upnpService, filter);
        this.upnpService = upnpService;
    }

    private Map<Action<LocalService<DataAdapter>>, ActionExecutor> createActionExecutors(UPnPAction[] actions) {
        Map<Action<LocalService<DataAdapter>>, ActionExecutor> executors = new HashMap();

        if (actions != null) {
            for (UPnPAction action : actions) {
                List<ActionArgument<LocalService<DataAdapter>>> list = new ArrayList();

                String[] names;

                names = action.getInputArgumentNames();
                if (names != null) {
                    for (String name : names) {
                        UPnPStateVariable variable = action.getStateVariable(name);
                        ActionArgument<LocalService<DataAdapter>> argument =
                                new ActionArgument<LocalService<DataAdapter>>(
                                        name,
                                        variable.getName(),
                                        ActionArgument.Direction.IN,
                                        false
                                );
                        list.add(argument);
                    }
                }

                names = action.getOutputArgumentNames();
                if (names != null) {
                    for (String name : names) {
                        UPnPStateVariable variable = action.getStateVariable(name);
                        ActionArgument<LocalService<DataAdapter>> argument =
                                new ActionArgument<LocalService<DataAdapter>>(
                                        name,
                                        variable.getName(),
                                        ActionArgument.Direction.OUT,
                                        false
                                );
                        list.add(argument);
                    }
                }

                Action<LocalService<DataAdapter>> local = new Action<LocalService<DataAdapter>>(
                        action.getName(),
                        list.toArray(new ActionArgument[list.size()])
                );

                executors.put(local, new UPnPActionExecutor(action));
            }
        }

        return executors;
    }

    private Map<StateVariable<LocalService<DataAdapter>>, StateVariableAccessor> createStateVariableAccessors(UPnPStateVariable[] variables) {
        Map<StateVariable<LocalService<DataAdapter>>, StateVariableAccessor> map = new HashMap();

        if (variables != null) {
            for (UPnPStateVariable variable : variables) {

                Datatype<?> dataType = Datatype.Builtin.getByDescriptorName(variable.getUPnPDataType()).getDatatype();
                StateVariable<LocalService<DataAdapter>> local = new StateVariable<LocalService<DataAdapter>>(
                        variable.getName(),
                        new StateVariableTypeDetails(dataType),
                        new StateVariableEventDetails(variable.sendsEvents())
                );
                if (variable instanceof UPnPLocalStateVariable) {
                    map.put(local, new UPnPLocalStateVariableAccessor((UPnPLocalStateVariable) variable));
                } else {
                    map.put(local, new UPnPStateVariableAccessor(variable));
                }
            }
        }

        return map;

    }

    private Set<Class<?>> createStringConvertibleTypes() {
        Set<Class<?>> set = new HashSet<Class<?>>();

        set.add(Boolean.class);
        set.add(Byte.class);
        set.add(Integer.class);
        set.add(Long.class);
        set.add(Float.class);
        set.add(Double.class);
        set.add(Character.class);
        set.add(String.class);
        set.add(Date.class);

        return set;
    }

    private LocalService<DataAdapter>[] createServices(UPnPService[] services) throws InvalidValueException, ValidationException {
        List<LocalService<DataAdapter>> list = null;

        if (services != null) {
            list = new ArrayList<LocalService<DataAdapter>>();
            for (UPnPService service : services) {
                UPnPLocalServiceImpl<DataAdapter> local =
                        new UPnPLocalServiceImpl<DataAdapter>(
                                ServiceType.valueOf(service.getType()),
                                ServiceId.valueOf(service.getId()),
                                (Map) createActionExecutors(service.getActions()),
                                (Map) createStateVariableAccessors(service.getStateVariables()),
                                (Set) createStringConvertibleTypes(),
                                false
                        );

                //local.setManager(new UPnPServiceManager<DataAdapter>(local));
                local.setManager(new DefaultServiceManager<DataAdapter>(local, DataAdapter.class));

                list.add(local);
            }
        }

        return (list != null) ? list.toArray(new LocalService[list.size()]) : null;
    }

    private Icon[] createIcons(UPnPIcon[] icons) throws IOException, URISyntaxException {
        List<Icon> list = null;

        if (icons != null) {
            list = new ArrayList<Icon>();
            for (UPnPIcon icon : icons) {
                InputStream in = icon.getInputStream();
                if (in != null) {
                    Icon local =
                            new Icon(icon.getMimeType(),
                                     icon.getWidth(),
                                     icon.getHeight(),
                                     icon.getDepth(),
                                     UUID.randomUUID().toString(),
                                     in
                            );
                    list.add(local);
                }
            }
        }

        return (list != null) ? list.toArray(new Icon[list.size()]) : null;
    }


    private String getSafeString(Object object) {
        return (object != null) ? object.toString() : null;
    }

    private URI getSafeURI(Object object) {
        return (object != null) ? URI.create(object.toString()) : null;
    }

    private LocalDevice createDevice(UPnPDevice in) throws ValidationException, IOException, URISyntaxException {
        Dictionary<?, ?> descriptions = in.getDescriptions(null);
        DeviceIdentity identity =
                new DeviceIdentity(
                        new UDN(getSafeString(descriptions.get(UPnPDevice.UDN)))
                );

        DeviceType type =
                DeviceType.valueOf(getSafeString(descriptions.get(UPnPDevice.TYPE)));

        DeviceDetails details =
                new DeviceDetails(
                        getSafeString(descriptions.get(UPnPDevice.FRIENDLY_NAME)),
                        new ManufacturerDetails(
                                getSafeString(descriptions.get(UPnPDevice.MANUFACTURER)),
                                getSafeURI(descriptions.get(UPnPDevice.MANUFACTURER_URL))
                        ),
                        new ModelDetails(
                                getSafeString(descriptions.get(UPnPDevice.MODEL_NAME)),
                                getSafeString(descriptions.get(UPnPDevice.MODEL_DESCRIPTION)),
                                getSafeString(descriptions.get(UPnPDevice.MODEL_NUMBER)),
                                getSafeURI(descriptions.get(UPnPDevice.MODEL_URL))
                        ),
                        getSafeString(descriptions.get(UPnPDevice.SERIAL_NUMBER)),
                        getSafeString(descriptions.get(UPnPDevice.UPC)),
                        getSafeURI(descriptions.get(UPnPDevice.PRESENTATION_URL))
                );

        Icon[] icons = createIcons(in.getIcons(null));

        LocalService<DataAdapter>[] services = createServices(in.getServices());

        return new LocalDevice(identity, type, details, icons, services);
    }

    @Override
    public Object addingService(ServiceReference reference) {
		log.trace("ENTRY {}.{}: {}", this.getClass().getName(), "addingService", reference);
        UPnPDevice device = (UPnPDevice) super.addingService(reference);
        log.trace(device.toString());

        try {
            LocalDevice local = createDevice(device);
            if (local != null) {
                upnpService.getRegistry().addDevice(local);
                registrations.put(device, local);
            }

        } catch (ValidationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return device;
    }

    @Override
    public void removedService(ServiceReference reference, Object device) {
		log.trace("ENTRY {}.{}: {} {}", this.getClass().getName(), "removedService", reference, device);

        LocalDevice local = registrations.get(device);
        if (local != null) {
            upnpService.getRegistry().removeDevice(local);
            registrations.remove(device);
        }
    }
}

