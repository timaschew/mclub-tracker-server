/**
 * Project: MyForesterApp
 * 
 * File Created at 2013-8-1
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.datamining.rules

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import mclub.datamining.RuleState
import mclub.util.DateUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Rule interface
 * 
 * @author shawn
 *
 */
public interface Rule{
	/**
	 * the rule name
	 * @return
	 */
	public String getName();
	/**
	 *
	 * @param params
	 * @return
	 */
	public abstract int execute(Map<Object,Object> context);
}

/**
 * Abstract rule
 * @author shawn
 *
 */
public abstract class AbstractRule implements Rule{
	Logger log = LoggerFactory.getLogger(getClass());
	public static final String lastExcutionTimeStamp = "LastExecutionTimeStamp";

	public String getName(){
		return getClass().getSimpleName();
	}
	
	//=======================================================================
	// Load/Save device rule states
	//=======================================================================
	
	/**
	 * Save rule sate in database
	 * @param stateValue
	 */
	void saveState(String deviceId, Map<String,Object> stateValue){
		// serialize
		def builder = new JsonBuilder()
		builder(stateValue)
		def json = builder.toString()

		RuleState state = RuleState.findByDeviceIdAndRuleName(deviceId,this.getName());
		if(!state){
			state = new RuleState()
			state.ruleName = this.getName();
			state.deviceId = deviceId;
		}
		state.value = json;
		if(!state.save(flush:true)){
			log.error("Error saving rule state:${state.errors}")
		}
	}

	/**
	 * Load rules from database
	 * @return
	 */
	Map<String,Object> loadState(String deviceId){
		/*
		 RuleState state = RuleState.find(
		 "FROM RuleState rs WHERE rs.ruleName=:rn AND rs.lastUpdated > :yesterday AND rs.lastUpdated < :tomorrow",
		 [rn:this.getName(),yesterday:DateUtils.yesterday(),tomorrow:DateUtils.tomorrow()]
		 );
		 */
		RuleState state = RuleState.findByDeviceIdAndRuleName(deviceId,getName());

		if(!state){
			return [:];
		}

		if(state){
			String json = state.value;
			// deserialize
			def slurper = new JsonSlurper()
			try{
				def m = slurper.parseText(json);
				if(m){
					return m;
				}
			}catch(Exception e){
				log.error("Error loading rule state: ${e.message}");
			}
		}
		return [:];
	}

	def deviceRuleStateMap = [:];
	
	public Map<String,Object> load(String deviceId){
		def map = new HashMap(loadState(deviceId));
		deviceRuleStateMap[deviceId] = map;
		return map;
	}
	public void update(String deviceId, String key, Object value){
		def stateMap = deviceRuleStateMap[deviceId];
		if(!stateMap){
			stateMap = load(deviceId);
		}
		stateMap[key] = value;
	}
	public Map<String,Object> commit(String deviceId){
		def stateMap = deviceRuleStateMap[deviceId];
		if(stateMap){
			saveState(deviceId, stateMap);
			deviceRuleStateMap[deviceId] = null;
		}
	}
}
