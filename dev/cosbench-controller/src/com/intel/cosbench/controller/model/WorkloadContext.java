/** 
 
Copyright 2013 Intel Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/ 

package com.intel.cosbench.controller.model;

import java.util.*;
import java.util.concurrent.Future;

import com.intel.cosbench.bench.*;
import com.intel.cosbench.config.*;
import com.intel.cosbench.model.*;

public class WorkloadContext implements WorkloadInfo {

    private String id;
    private Date submitDate;
    private Date startDate;
    private Date stopDate;
    private volatile WorkloadState state;
    private StateRegistry stateHistory = new StateRegistry();
    private transient XmlConfig config;
    private transient volatile Future<?> future;

    private Workload workload;
    private transient volatile StageInfo currentStage;
    private StageRegistry stageRegistry;

    /* Report will be available after the workload is finished */
    private volatile Report report = null; // will be merged from stage reports

    private transient List<WorkloadListener> listeners = new ArrayList<WorkloadListener>();

    public WorkloadContext() {
        /* empty */
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    @Override
    public WorkloadState getState() {
        return state;
    }

    public void setState(WorkloadState state) {
        this.state = state;
        stateHistory.addState(state.name());
        if (WorkloadState.isRunning(state))
            fireWorkloadStarted();
        if (WorkloadState.isStopped(state))
            fireWorkloadStopped();
    }

    private void fireWorkloadStarted() {
        for (WorkloadListener listener : listeners)
            listener.workloadStarted(this);
    }

    private void fireWorkloadStopped() {
        if (report == null)
            report = mergeReport();
        for (WorkloadListener listener : listeners)
            listener.workloadStopped(this);
    }

    private Report mergeReport() {
        Report report = new Report();
        for (StageContext stage : stageRegistry) {
            int mid = 1;
            for (Metrics metrics : stage.getReport()) {
                Metrics clone = metrics.clone();
                String uuid = id + "-" + stage.getId() + "-" + mid++;
                clone.setName(uuid); // reset metrics name
                report.addMetrics(clone);
            }
        }
        return report;
    }

    @Override
    public StateInfo[] getStateHistory() {
        return stateHistory.getAllStates();
    }

    @Override
    public XmlConfig getConfig() {
        return config;
    }

    public void setConfig(XmlConfig config) {
        this.config = config;
    }

    public Future<?> getFuture() {
        return future;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    @Override
    public Workload getWorkload() {
        return workload;
    }

    public void setWorkload(Workload workload) {
        this.workload = workload;
    }

    @Override
    public String[] getAllOperations() {
        Set<String> ops = new LinkedHashSet<String>();
        for (Stage stage : workload.getWorkflow())
            for (Work work : stage)
                for (Operation op : work)
                    ops.add(op.getType());
        return ops.toArray(new String[ops.size()]);
    }

    @Override
    public StageInfo getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(StageInfo currentStage) {
        this.currentStage = currentStage;
    }

    public StageRegistry getStageRegistry() {
        return stageRegistry;
    }

    @Override
    public int getStageCount() {
        return stageRegistry.getSize();
    }

    @Override
    public StageInfo getStageInfo(String id) {
        for (StageInfo info : stageRegistry)
            if (info.getId().equals(id))
                return info;
        return null;
    }

    @Override
    public StageInfo[] getStageInfos() {
        return stageRegistry.getAllStages();
    }

    public void setStageRegistry(StageRegistry stageRegistry) {
        this.stageRegistry = stageRegistry;
    }

    @Override
    public int getSnapshotCount() {
        int total = 0;
        for (StageInfo info : stageRegistry)
            total += info.getSnapshotCount();
        return total;
    }

    @Override
    public Snapshot getSnapshot() {
        if (currentStage == null)
            return new Snapshot();
        return currentStage.getSnapshot();
    }

    @Override
    public Report getReport() {
        return report != null ? report : new Report();
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public void addListener(WorkloadListener listener) {
        listeners.add(listener);
    }

    @Override
    public void disposeRuntime() {
        for (StageContext stage : stageRegistry)
            stage.disposeRuntime();
        config = null;
        future = null;
        currentStage = null;
        listeners = null;
    }

}
