/**
* Embargo Utility is a script that improves the AccessStep functionality, easing the work 
* of the user by the selection of friendly and intuitive graphics elements to allocate the embargo.
* 
* EXAMPLE OF USE:
*   //Add a group of radio buttons in a 'li' element under the selector indicated.
* 	$(document).ready(function(){
*		var radioButtonGenerator = doTest();
*		radioButtonGenerator.putRadioButtons('#aspect_submission_StepTransformer_list_submit-add-item-policy  ol', 'li',true);
*		if(confirm("Wants to add more?")){
*			var newPeriods = new Array();
*			newPeriods[0] = ["9 months",180];
*			newPeriods[1] = ["10 months",210];
*			radioButtonGenerator.addRadioButtons(newPeriods,3,1);
*			//manually refresh radio groups in page
*			radioButtonGenerator.putRadioButtons('#aspect_submission_StepTransformer_list_submit-add-item-policy ol','li',true,true);
*		}
*
*	//Removes the radio buttons relative to the positions 1 and 4.
* 	$(document).ready(function(){
*		var radioButtonGenerator = doTest();
*		radioButtonGenerator.putRadioButtons('#aspect_submission_StepTransformer_list_submit-add-item-policy  ol', 'li',true);
*		radioButtonGenerator.removeRadioButtonsByPos([1][4]);
*		//manually refresh radio groups in page
*		radioButtonGenerator.putRadioButtons('#aspect_submission_StepTransformer_list_submit-add-item-policy  ol', 'li',true);
*	}
*
*
*/

/**
 * Function for testing.
 * @returns a RadioButtonGenerator with values preloaded.
 */
function doTest(){
	var periods = new Array();
	periods[0] = ["No embargo",0];
	periods[1] = ["3 months",60];
	periods[2] = ["6 months",120];
	periods[3] = ["1 year",365];
	periods[4] = ["2 years",730];
	
	var radioButtonGenerator = getRadioButtonGenerator('radio_period');
	radioButtonGenerator.setRadioButtons(periods);
	
	return radioButtonGenerator;
}

/**
 * Constructor of a RadioButtonGenerator
 * 
 * @param groupName is the name of the radio buttons group. Grouping the radio buttons lets that only one radio be selected at once.
 * @returns RadioButtonGenerator
 */
function getRadioButtonGenerator(groupNameParam){
	var radioButtonGenerator = {
			groupName: groupNameParam,
			
			/**
			 * radioList is an array o arrays. Each array element inside must have 2 elements: 
			 * a 'key' (first element) and a 'value' (second element). P.e.: [["elem1",12],["eleme2",10],...] 
			 * 
			 */
			radioList: null,
	
			/**
			 * By default, the checked element of the radio button group is the element in the position 0.
			 */
			indexCheckedElement: null,
			/**
			 * Initial set of the RadioButtonGenerator radioList.
			 * 
			 * @param listOfRadio -- is a "JavaScript array of array" that contains the list of radio to set, 
			 * 		each element composed by 'key:value'. The 'key' maps to the 'label' of the radio, and the 'value'
			 * 		maps to the 'value radio attribute'.
			 * @param chekedElement(optional) -- index that indicates what is the radio checked by default
			 * @returns nothing
			 */
			setRadioButtons: function(listOfRadio, checkedElement){
				this.radioList = listOfRadio;
				if(checkedElement != null && checkedElement >= 0 && checkedElement < this.radioList){ 
					this.indexCheckedElement = checkedElement; 
				}
				
			},
			
			/**
			 * Create html inputs. These are radio buttons.
			 * 
			 * @param selector -- tell to the RadioButtonGenerator where put the radio buttons. Use a JQuery selector.
			 * @param containerElement -- specifies the html element that will contain the radioButtons
			 * @param inline (boolean) -- tells if the radio must be put in-line or not.
			 * @param deleteExisting (optional)(boolean) -- if true, deletes existing radio elements under the 'selector' indicated.
			 * @returns nothing
			 */
			putRadioButtons: function(selector, containerElement, inline, deleteExisting){
				//there are a matching using this selector?
				if($(selector).length){
					var br = (inline)?'<br/>':'';
					var radioName = this.groupName;
					var indexRadioChecked = this.indexCheckedElement;
					
					//check if delete existing elements
					if(deleteExisting != null && deleteExisting){
						$(selector + ' > '+containerElement+'[id="radio_buttons_group_'+radioName+'"]').remove();
					}
					
					//create the container
					$(selector).prepend('<'+containerElement+' id="radio_buttons_group_'+radioName+'">');
					
					this.radioList.forEach(function(elementInList, index, array){
						var checked=(indexRadioChecked != null && indexRadioChecked == index)?"checked":"";
						$(selector + ' > '+containerElement+'[id="radio_buttons_group_'+radioName+'"]').append('<input type="RADIO" id="radio_element_pos_'+index+'" name="'+ radioName +'" value="'+elementInList[1]+'" '+ checked +' /> <span>'+elementInList[0]+' </span>'+br);
					});
				}
			},
			
			/**
			 * Add elements to the radioList, if it is set.
			 * 
			 * @param listOfRadioToAdd -- is a 'JavaScript dictionary'. Same considerations as in #setRadioButtons
			 * @param position -- the position in the radioList array where to add the elements
			 * @param newCheckedElementIndex -- index relative to an element the listOfRadioToAdd. If set, this element is set as the new checked element in the radioList
			 * @returns nothing
			 * 
			 * TODO: make a final synchronization, updating the existing groups of radio buttons in the page.
			 */
			addRadioButtons: function(listOfRadioToAdd,position,newCheckedElementIndex){
				if(this.radioList != null){	
					var definitivePosition;
					//Verify if the position to add an element is in the limits of the radioList array.
					if (position > 0){
						definitivePosition = (position < this.radioList.length)?position:this.radioList.length;
					}else{
						definitivePosition = 0;
					}
					var definitiveCheckedIndex = (newCheckedElementIndex != null && newCheckedElementIndex >= 0 && newCheckedElementIndex <= (listOfRadioToAdd.length-1))?newCheckedElementIndex:null;
					for (i=0; i < listOfRadioToAdd.length; i++){
						this.radioList.splice(definitivePosition, 0, listOfRadioToAdd[i]);
						if(definitiveCheckedIndex != null && definitiveCheckedIndex == i){ this.indexCheckedElement=definitivePosition;}
						definitivePosition++;
					}
				}
			},
			
			/**
			 * Remove the radio buttons that has a correspondence with the position specified.  
			 * @param listOfPositionToRemove -- is an array that contains possible element's positions existing in the radioList array.
			 * @param newCheckedElement(optional) -- applies once the elements were removed. Sets a new indexCheckedElement.
			 * @returns nothing
			 * 
			 * TODO: make a final synchronization, updating the existing groups of radio buttons in the page.
			 */
			removeRadioButtonsByPos: function(listOfPositionToRemove,newCheckedElementIndex){
				var definitivePosition;
				var position;
				for(i=0; i < listOfPositionToRemove.length; i++){
					//Verify if the position to add an element is in the limits of the radioList array.
					position = listOfPositionToRemove[i];
					definitivePosition = (position >= 0 && position < this.radioList.length)?position:-1;
					if(definitivePosition >= 0){
						this.radioList.splice(definitivePosition,1);
						//If the indexCheckedElement configured for this radioButtonGenerator is deleted, then resets to 0.
						if(this.indexCheckedElement == definitivePosition){this.indexCheckedElement = 0;}
					}
				}
				if(newCheckedElementIndex!= null && newCheckedElementIndex >= 0 &&  newCheckedElementIndex < this.radioList.length){
					this.indexCheckedElement = newCheckedElementIndex;
				}
			},
			
			/**
			 * Attaches an event controller to a radio button created by the radioButtonGenerator.
			 * @param index -- array that contains numerical positions relatives to a button in the radioList contained in the radioButtonGenerator.
			 * @param controller (function) -- a function that handles the event specified. 
			 * @param event (string) -- name the event to handle
			 * @param selector (optional) -- JQuery selector to specify a given context. If not specify, the controller will be
			 * 			attached to every button related to the element in the radioList.
			 * @returns nothing
			 */
			addControllerToRadioButton: function(index, controller, event, selector){
				for(i=0; i < index.length; i++){
					if(index[i] >= 0 && index[i] < this.radioList.length){
						var definitiveSelector = (selector != null)?selector:'input[type="RADIO"][id="radio_element_pos_'+ index[i] +'"][name="'+ this.groupName +'"]';
						$(definitiveSelector).bind(event,controller);
					}
				}
			},
			/**
			 * Adds or alters (reset) an attribute to a set of radio buttons specified.
			 * @index -- array that contains numerical positions relatives to a button in the radioList contained in the radioButtonGenerator.
			 * @name -- name of the attribute to change/add
			 * @value -- value of the attribute.
			 * @returns nothing
			 */
			addAtributeToRadioButton: function(index, name, value){
				for(i=0; i < index.length; i++){
					if(index[i] >= 0 && index[i] < this.radioList.length){
						$('input[type="RADIO"][id="radio_element_pos_'+ index[i] +'"][name="'+ this.groupName +'"]').attr(name,value);
					}
				}
			},
			
			/**
			 * Returns all the instances related with an element in the radioList.
			 */
			getRadioButtonInDocument: function(index){
				var selector = 'input[type="RADIO"][id="radio_element_pos_'+ index +'"][name="'+ this.groupName +'"]';
				return $(selector);
			}
	}
	
	return radioButtonGenerator;
}

/**
 * Make read only the field that match the selector
 */
function makeFieldReadonly(selector){
	$(selector).prop("readonly",true);
}

/**
 * Calculate the embargo end date.
 * @param daysToAdd -- days to add to current date
 * @param baseDate (optional)(Date) -- the date from where calculate the embargo end date. If not set, calculate from current date.
 * @returns a Date -- the embargo end date caculated.
 */
function calculateEmbargoEndDate(daysToAdd,baseDate){
	var definitiveBaseDate = (baseDate != null)? baseDate : new Date();
	definitiveBaseDate.setDate(definitiveBaseDate.getDate() + parseInt(daysToAdd))
	return definitiveBaseDate;
}
