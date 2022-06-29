/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner

import io.github.wimdeblauwe.hsbt.mvc.HtmxRequest
import io.github.wimdeblauwe.hsbt.mvc.HtmxResponse
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.samples.petclinic.visit.VisitRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import javax.validation.Valid
import kotlin.random.Random

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Antoine Rey
 */
@Controller
class OwnerController(val owners: OwnerRepository, val visits: VisitRepository) {

    val VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm"

    @InitBinder
    fun setAllowedFields(dataBinder: WebDataBinder) {
        dataBinder.setDisallowedFields("id")
    }

    @EventListener
    fun onReady(readyEvent: ApplicationReadyEvent) {
        repeat(150) {
            owners.save(Owner().apply {
                address = "Main Street #${Random.nextInt(1000)}"
                city = "Town ${Random.nextInt(200)}"
                telephone = "5555555555"
                firstName = "Guy${it}"
                lastName = when(Random.nextInt(4)) {
                    0 -> "Smith"
                    1 -> "Wilson"
                    2 -> "Jones"
                    3 -> "Washington"
                    4 -> "Lastname"
                    else -> "Lastestname"
                }
            })
        }
    }

    @GetMapping("/owners/new")
    fun initCreationForm(model: MutableMap<String, Any>): String {
        val owner = Owner()
        model["owner"] = owner
        return VIEWS_OWNER_CREATE_OR_UPDATE_FORM
    }

    @PostMapping("/owners/new")
    fun processCreationForm(@Valid owner: Owner, result: BindingResult): String {
        return if (result.hasErrors()) {
            VIEWS_OWNER_CREATE_OR_UPDATE_FORM
        } else {
            owners.save(owner)
            "redirect:/owners/" + owner.id
        }
    }

    @GetMapping("/owners/find")
    fun initFindForm(htmxReq: HtmxRequest, model: MutableMap<String, Any>): String {
        model["owner"] = Owner()
        return if(htmxReq.isHtmxRequest) {
            "owners/findOwners :: body"
        } else {
            "owners/findOwners"
        }
    }

    @GetMapping("/owners")
    fun processFindForm(owner: Owner, result: BindingResult, model: MutableMap<String, Any>, hxRequest: HtmxRequest): HtmxResponse {
        val resp = HtmxResponse()
        // find owners by last name
        val results = owners.findByLastName(owner.lastName)
        when {
            results.isEmpty() -> {
                // no owners found
                result.rejectValue("lastName", "notFound", "not found")
                if(hxRequest.isHtmxRequest) resp.addTemplate("owners/findOwners :: body") else resp.addTemplate("owners/findOwners")
            }
            results.size == 1 -> {
                // 1 owner found
                resp.browserRedirect("/owners/" + results.first().id)
            }
            else -> {
                // multiple owners found
                model["selections"] = results
                if(hxRequest.isHtmxRequest) resp.addTemplate("owners/ownersList :: body") else resp.addTemplate("owners/ownersList")
            }
        }
        return resp
    }

    @GetMapping("/owners/{ownerId}/edit")
    fun initUpdateOwnerForm(@PathVariable("ownerId") ownerId: Int, model: Model): String {
        val owner = owners.findById(ownerId)
        model.addAttribute(owner)
        return VIEWS_OWNER_CREATE_OR_UPDATE_FORM
    }

    @PostMapping("/owners/{ownerId}/edit")
    fun processUpdateOwnerForm(@Valid owner: Owner, result: BindingResult, @PathVariable("ownerId") ownerId: Int): String {
        return if (result.hasErrors()) {
            VIEWS_OWNER_CREATE_OR_UPDATE_FORM
        } else {
            owner.id = ownerId
            this.owners.save(owner)
            "redirect:/owners/{ownerId}"
        }
    }

    /**
     * Custom handler for displaying an owner.
     *
     * @param ownerId the ID of the owner to display
     * @return the view
     */
    @GetMapping("/owners/{ownerId}")
    fun showOwner(@PathVariable("ownerId") ownerId: Int, model: Model): String {
        val owner = this.owners.findById(ownerId)
        for (pet in owner.getPets()) {
            pet.visits = visits.findByPetId(pet.id!!)
        }
        model.addAttribute(owner)
        return "owners/ownerDetails"
    }

}

